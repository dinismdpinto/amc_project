package projeto_amc;

import java.util.ArrayList;
import java.util.LinkedList;

public class Grafosorientados {

    int n; 
    ArrayList<LinkedList<Integer>> adj; // Listas de adjacência (quem são os filhos de quem)

    public Grafosorientados(int n) {
        this.n = n;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            adj.add(new LinkedList<>());
    }

    // --- MÉTODOS DE MANIPULAÇÃO DO GRAFO ---
    
    // Adiciona uma seta u -> v
    public void add_edge(int u, int v) {
        if (!adj.get(u).contains(v)) adj.get(u).add(v);
    }

    // Remove a seta u -> v
    public void remove_edge(int u, int v) {
        adj.get(u).remove((Integer) v);
    }

    // Inverte a seta u -> v para v -> u
    public void invert_edge(int u, int v) {
        remove_edge(u, v);
        add_edge(v, u);
    }

    // Retorna a lista de pais de um nó (quem aponta para 'node')
    public LinkedList<Integer> parents(int node) {
        LinkedList<Integer> p = new LinkedList<>();
        for (int i = 0; i < n; i++)
            if (adj.get(i).contains(node)) p.add(i);
        return p;
    }

    // Verifica se existe caminho de u para v (usado para detetar ciclos)
    public boolean connected(int u, int v) {
        boolean[] visited = new boolean[n];
        return dfsConnected(u, v, visited);
    }

    // Algoritmo de busca em profundidade (DFS) auxiliar
    private boolean dfsConnected(int cur, int target, boolean[] visited) {
        if (cur == target) return true;
        visited[cur] = true;
        for (int nxt : adj.get(cur))
            if (!visited[nxt] && dfsConnected(nxt, target, visited)) return true;
        return false;
    }

    // --- ALGORITMO GREEDY HILL CLIMBER (APRENDIZAGEM) ---
    // É aqui que o grafo é construído automaticamente
    public void aprender(Amostra T) {
        boolean melhorou = true;
        int indiceClasse = T.dim() - 1; // A classe é sempre a última coluna

        while (melhorou) {
            melhorou = false;
            double melhorDelta = 0.0;
            int op = -1, bestU = -1, bestV = -1; // op: 0=remover, 1=inverter, 2=adicionar

            // Testa todas as combinações possíveis de pares de nós (u, v)
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    if (u == v) continue; // Não pode haver laços (seta para si mesmo)

                    // REGRA 1: A variável Classe (v) nunca pode ter pais.
                    // A classe causa os sintomas, não o contrário.
                    if (v == indiceClasse) continue;

                    boolean existe = parents(v).contains(u);

                    if (!existe) { 
                        // Tentar ADICIONAR aresta u -> v
                        
                        // REGRA 2: Limite máximo de 2 pais por nó (para evitar complexidade excessiva)
                        // Ignoramos esta restrição se quisermos uma rede complexa, mas para BNC simples ajuda.
                        if (parents(v).size() >= 2) continue;

                        double d = MDLdelta(T, u, v, 2);
                        if (d > melhorDelta) { melhorDelta = d; bestU = u; bestV = v; op = 2; }
                    } else { 
                        // Tentar REMOVER aresta u -> v
                        double dRem = MDLdelta(T, u, v, 0);
                        if (dRem > melhorDelta) { melhorDelta = dRem; bestU = u; bestV = v; op = 0; }
                        
                        // Tentar INVERTER aresta u -> v (vira v -> u)
                        // Se invertermos, 'u' passa a ser o filho. Logo, 'u' não pode ser a Classe.
                        if (u != indiceClasse) {
                             double dInv = MDLdelta(T, u, v, 1);
                             if (dInv > melhorDelta) { melhorDelta = dInv; bestU = u; bestV = v; op = 1; }
                        }
                    }
                }
            }

            // Se encontrámos uma operação que melhora o score (MDL), aplicamo-la.
            if (melhorDelta > 0.0001) { // 0.0001 é uma pequena margem para evitar loops infinitos com valores iguais
                melhorou = true;
                if (op == 0) remove_edge(bestU, bestV);
                else if (op == 1) invert_edge(bestU, bestV);
                else if (op == 2) add_edge(bestU, bestV);
            }
        }
    }

    // --- CÁLCULO DO SCORE MDL ---
    // Calcula quanto o score muda se aplicarmos uma operação, sem recalcular a rede toda.
    public double MDLdelta(Amostra T, int u, int v, int operacao) {
        double delta = Double.NEGATIVE_INFINITY;
        
        if (operacao == 2) { // Adicionar u -> v
            // Se criar ciclo, é proibido (-Infinito)
            if (connected(v, u)) return Double.NEGATIVE_INFINITY; 
            
            double sAntes = scoreLocal(v, T);
            add_edge(u, v); // Simula adição
            double sDepois = scoreLocal(v, T);
            remove_edge(u, v); // Desfaz
            
            delta = sDepois - sAntes;
        } 
        else if (operacao == 0) { // Remover u -> v
            double sAntes = scoreLocal(v, T);
            remove_edge(u, v); // Simula remoção
            double sDepois = scoreLocal(v, T);
            add_edge(u, v); // Desfaz
            
            delta = sDepois - sAntes;
        } 
        else if (operacao == 1) { // Inverter u -> v
            remove_edge(u, v);
            // Verifica se a nova aresta v->u criaria ciclo
            if (connected(u, v)) { add_edge(u, v); return Double.NEGATIVE_INFINITY; }
            add_edge(u, v); // Repor para calcular score inicial

            double sAntes = scoreLocal(u, T) + scoreLocal(v, T);
            invert_edge(u, v); // Simula inversão
            double sDepois = scoreLocal(u, T) + scoreLocal(v, T);
            invert_edge(v, u); // Desfaz
            
            delta = sDepois - sAntes;
        }
        return delta;
    }

    // Calcula o score MDL de um único nó Xi
    private double scoreLocal(int Xi, Amostra T) {
        int m = T.length();
        
        // 1. Log-Likelihood (Precisão): m * Informação Mútua Condicional
        double logLikelihood = m * informacaoMutuaCondicional(T, Xi);

        // 2. Penalização (Complexidade): (log2(m) / 2) * número de parâmetros
        int C = T.dim() - 1; 
        int dimXi = T.domain(Xi); // Quantos valores Xi pode assumir
        int dimC = T.domain(C);   // Quantas classes existem
        
        // q_i é o produto dos domínios dos pais
        int qi = 1;
        for (int pai : parents(Xi)) qi *= T.domain(pai);
        
        // Fórmula de parâmetros para BNC: (r_i - 1) * q_i * |C|
        // Referência: Friedman et al. (1997) ou conforme apontamentos
        int params = (dimXi - 1) * qi * dimC;
        
        // ATENÇÃO AOS LOGARITMOS: Penalização usa Log base 2
        double log2m = Math.log(m) / Math.log(2); 
        double penalizacao = (log2m / 2.0) * params;

        return logLikelihood - penalizacao;
    }

    // --- CÁLCULO DA INFORMAÇÃO MÚTUA CONDICIONAL (IMC) ---
    private double informacaoMutuaCondicional(Amostra T, int Xi) {
        int C = T.dim() - 1;              
        LinkedList<Integer> pais = parents(Xi);
        int m = T.length();

        // O IMC no BNC calcula a dependência entre Xi, os seus Pais, e a Classe C.
        // Array de variáveis: [Pais..., Xi, C]
        int[] vars = new int[pais.size() + 2];
        for (int i = 0; i < pais.size(); i++) vars[i] = pais.get(i);
        vars[pais.size()] = Xi;
        vars[pais.size() + 1] = C; 

        return percorreIMC(T, vars, new int[vars.length], 0, m);
    }

    // Função recursiva para somar sobre todos os valores possíveis das variáveis
    private double percorreIMC(Amostra T, int[] vars, int[] vals, int idx, int m) {
        if (idx == vars.length) {
            // Contagem conjunta N(x, parent(x), c)
            double n_xyz = T.count(vars, vals);
            if (n_xyz == 0) return 0.0;

            // Variáveis para os termos do denominador
            // vars_xc: Xi e C
            int[] vars_xc = { vars[vars.length - 2], vars[vars.length - 1] }; 
            int[] vals_xc = { vals[vars.length - 2], vals[vars.length - 1] };
            
            // vars_wc: Pais e C
            int[] vars_wc = new int[vars.length - 1];
            int[] vals_wc = new int[vals.length - 1];
            System.arraycopy(vars, 0, vars_wc, 0, vars.length - 1); 
            System.arraycopy(vals, 0, vals_wc, 0, vals.length - 1);

            double n_xc = T.count(vars_xc, vals_xc);
            double n_wc = T.count(vars_wc, vals_wc);
            
            // vars_c: C apenas
            int C = vars[vars.length - 1];
            double n_c = T.count(C, vals[vars.length - 1]);

            // Probabilidades
            double P_xyz = n_xyz / m;
            double P_xc  = n_xc  / m;
            double P_wc  = n_wc  / m;
            double P_c   = n_c   / m;

            // ATENÇÃO AOS LOGARITMOS: IMC usa Log Natural (Math.log)
            // Fórmula: P(xyz) * log [ (P(xyz) * P(c)) / (P(xc) * P(wc)) ]
            return P_xyz * Math.log((P_xyz * P_c) / (P_xc * P_wc));
        }

        double sum = 0.0;
        // Percorre todos os valores possíveis para a variável atual
        for (int v = 0; v < T.domain(vars[idx]); v++) {
            vals[idx] = v;
            sum += percorreIMC(T, vars, vals, idx + 1, m);
        }
        return sum;
    }
}