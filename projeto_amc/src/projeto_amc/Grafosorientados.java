package projeto_amc;

import java.util.ArrayList;
import java.util.LinkedList;

public class Grafosorientados {

    int n; 
    ArrayList<LinkedList<Integer>> adj; // Listas de adjacência

    public Grafosorientados(int n) {
        this.n = n;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            adj.add(new LinkedList<>());
    }

    // --- MÉTODOS DE MANIPULAÇÃO DO GRAFO ---
    
    public void add_edge(int u, int v) {
        if (!adj.get(u).contains(v)) adj.get(u).add(v);
    }

    public void remove_edge(int u, int v) {
        adj.get(u).remove((Integer) v);
    }

    public void invert_edge(int u, int v) {
        remove_edge(u, v);
        add_edge(v, u);
    }

    public LinkedList<Integer> parents(int node) {
        LinkedList<Integer> p = new LinkedList<>();
        for (int i = 0; i < n; i++)
            if (adj.get(i).contains(node)) p.add(i);
        return p;
    }

    public boolean connected(int u, int v) {
        boolean[] visited = new boolean[n];
        return dfsConnected(u, v, visited);
    }

    private boolean dfsConnected(int cur, int target, boolean[] visited) {
        if (cur == target) return true;
        visited[cur] = true;
        for (int nxt : adj.get(cur))
            if (!visited[nxt] && dfsConnected(nxt, target, visited)) return true;
        return false;
    }

    // --- ALGORITMO DE APRENDIZAGEM (HILL CLIMBING) ---

    // Sobrecarga: Se não especificar k, usa 2 por defeito
    public void aprender(Amostra T) {
        aprender(T, 2);
    }

    // LÓGICA ATUALIZADA:
    // k = limite de pais que são OUTROS ATRIBUTOS.
    // A Classe (variável alvo) pode ser adicionada como pai extra.
    // Limite total real = k + 1 (se a classe for pai).
    public void aprender(Amostra T, int k) {
        boolean melhorou = true;
        int indiceClasse = T.dim() - 1; // A classe é sempre a última coluna

        while (melhorou) {
            melhorou = false;
            double melhorDelta = 0.0;
            int op = -1, bestU = -1, bestV = -1; // 0=remover, 1=inverter, 2=adicionar

            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    if (u == v) continue; 
                    
                    // REGRA: A Classe nunca pode ser filha (nó V nunca é a classe)
                    if (v == indiceClasse) continue;

                    boolean existe = parents(v).contains(u);

                    if (!existe) { 
                        // --- TENTAR ADICIONAR (u -> v) ---
                        boolean podeAdicionar = false;

                        if (u == indiceClasse) {
                            // Se o pai proposto é a CLASSE:
                            // É sempre permitido tentar (não ocupa slot de 'k' atributos)
                            podeAdicionar = true;
                        } else {
                            // Se o pai proposto é um ATRIBUTO:
                            // Só podemos adicionar se o nó v ainda tiver "vagas" para atributos
                            if (contarPaisAtributos(v, indiceClasse) < k) {
                                podeAdicionar = true;
                            }
                        }

                        if (podeAdicionar) {
                            // O MDLdelta verifica internamente se cria ciclos (connected)
                            double d = MDLdelta(T, u, v, 2);
                            if (d > melhorDelta) { melhorDelta = d; bestU = u; bestV = v; op = 2; }
                        }

                    } else { 
                        // --- TENTAR REMOVER (u -> v) ---
                        // Remover é sempre permitido, não viola limites
                        double dRem = MDLdelta(T, u, v, 0);
                        if (dRem > melhorDelta) { melhorDelta = dRem; bestU = u; bestV = v; op = 0; }
                        
                        // --- TENTAR INVERTER (u -> v passa a ser v -> u) ---
                        // Ao inverter, o nó 'u' vai ganhar um novo pai ('v').
                        // Temos de verificar se 'u' tem espaço.
                        
                        if (u != indiceClasse) { // 'u' não pode ser a classe (classe não tem pais)
                            boolean podeInverter = false;

                            if (v == indiceClasse) {
                                // Se vamos receber a CLASSE como pai, é permitido (slot extra)
                                podeInverter = true;
                            } else {
                                // Se vamos receber um ATRIBUTO como pai, verificamos o limite k
                                if (contarPaisAtributos(u, indiceClasse) < k) {
                                    podeInverter = true;
                                }
                            }

                            if (podeInverter) {
                                double dInv = MDLdelta(T, u, v, 1);
                                if (dInv > melhorDelta) { melhorDelta = dInv; bestU = u; bestV = v; op = 1; }
                            }
                        }
                    }
                }
            }

            // Aplica a melhor operação encontrada nesta iteração
            if (melhorDelta > 0.0001) { 
                melhorou = true;
                if (op == 0) remove_edge(bestU, bestV);
                else if (op == 1) invert_edge(bestU, bestV);
                else if (op == 2) add_edge(bestU, bestV);
            }
        }
    }

    // --- NOVO MÉTODO AUXILIAR ---
    // Conta quantos pais o nó tem, IGNORANDO a Classe.
    private int contarPaisAtributos(int node, int indiceClasse) {
        int count = 0;
        LinkedList<Integer> pais = parents(node);
        for (Integer pai : pais) {
            if (pai != indiceClasse) {
                count++;
            }
        }
        return count;
    }
    
    // --- CÁLCULO DO SCORE MDL GLOBAL ---
    public double MDL(Amostra T) {
        double total = 0.0;
        for(int i=0; i<n; i++) total += scoreLocal(i, T);
        return total;
    }

    // --- CÁLCULO DO SCORE MDL DELTA ---
    public double MDLdelta(Amostra T, int u, int v, int operacao) {
        double delta = Double.NEGATIVE_INFINITY;
        
        if (operacao == 2) { // Adicionar u -> v
            if (connected(v, u)) return Double.NEGATIVE_INFINITY; // Ciclo!
            
            double sAntes = scoreLocal(v, T);
            add_edge(u, v); 
            double sDepois = scoreLocal(v, T);
            remove_edge(u, v); 
            
            delta = sDepois - sAntes;
        } 
        else if (operacao == 0) { // Remover u -> v
            double sAntes = scoreLocal(v, T);
            remove_edge(u, v); 
            double sDepois = scoreLocal(v, T);
            add_edge(u, v); 
            
            delta = sDepois - sAntes;
        } 
        else if (operacao == 1) { // Inverter u -> v
            remove_edge(u, v);
            if (connected(u, v)) { add_edge(u, v); return Double.NEGATIVE_INFINITY; }
            add_edge(u, v); 

            double sAntes = scoreLocal(u, T) + scoreLocal(v, T);
            invert_edge(u, v); 
            double sDepois = scoreLocal(u, T) + scoreLocal(v, T);
            invert_edge(v, u); 
            
            delta = sDepois - sAntes;
        }
        return delta;
    }

    // Calcula o score MDL de um único nó Xi
    private double scoreLocal(int Xi, Amostra T) {
        int m = T.length();
        
        // 1. Log-Likelihood
        double logLikelihood = m * informacaoMutuaCondicional(T, Xi);

        // 2. Penalização
        int C = T.dim() - 1; 
        int dimXi = T.domain(Xi); 
        int dimC = T.domain(C);     
        
        int qi = 1;
        for (int pai : parents(Xi)) qi *= T.domain(pai);
        
        int params = (dimXi - 1) * qi * dimC;
        
        double log2m = Math.log(m) / Math.log(2); 
        double penalizacao = (log2m / 2.0) * params;

        return logLikelihood - penalizacao;
    }

    // --- CÁLCULO DA INFORMAÇÃO MÚTUA CONDICIONAL ---
    private double informacaoMutuaCondicional(Amostra T, int Xi) {
        int C = T.dim() - 1;                
        LinkedList<Integer> pais = parents(Xi);
        int m = T.length();

        int[] vars = new int[pais.size() + 2];
        for (int i = 0; i < pais.size(); i++) vars[i] = pais.get(i);
        vars[pais.size()] = Xi;
        vars[pais.size() + 1] = C; 

        return percorreIMC(T, vars, new int[vars.length], 0, m);
    }

    private double percorreIMC(Amostra T, int[] vars, int[] vals, int idx, int m) {
        if (idx == vars.length) {
            double n_xyz = T.count(vars, vals); 
            if (n_xyz == 0) return 0.0;

            int[] vars_xc = { vars[vars.length - 2], vars[vars.length - 1] }; 
            int[] vals_xc = { vals[vars.length - 2], vals[vars.length - 1] };
            
            int[] vars_wc = new int[vars.length - 1];
            int[] vals_wc = new int[vals.length - 1];
            System.arraycopy(vars, 0, vars_wc, 0, vars.length - 1); 
            System.arraycopy(vals, 0, vals_wc, 0, vals.length - 1);

            double n_xc = T.count(vars_xc, vals_xc);
            double n_wc = T.count(vars_wc, vals_wc);
            int C = vars[vars.length - 1];
            double n_c = T.count(C, vals[vars.length - 1]);

            double P_xyz = n_xyz / m;
            double P_xc  = n_xc  / m;
            double P_wc  = n_wc  / m;
            double P_c   = n_c   / m;

            return P_xyz * Math.log((P_xyz * P_c) / (P_xc * P_wc));
        }

        double sum = 0.0;
        for (int v = 0; v < T.domain(vars[idx]); v++) {
            vals[idx] = v;
            sum += percorreIMC(T, vars, vals, idx + 1, m);
        }
        return sum;
    }
}