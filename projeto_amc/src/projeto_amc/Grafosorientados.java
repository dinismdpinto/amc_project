package projeto_amc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;

public class Grafosorientados {

    int n; 
    ArrayList<LinkedList<Integer>> adj; 

    // --- CACHE DE ALTA PERFORMANCE ---
    // [Var1][Var2][Valor1][Valor2] -> Guarda contagens de pares instantaneamente
    private int[][][][] cachePares; 
    private int[][] cacheSimples; 
    
    // Cache de scores para evitar recalcular nós que não foram tocados
    private double[] cacheScoresLocais;

    public Grafosorientados(int n) {
        this.n = n;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new LinkedList<>());
    }

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

    // DFS para verificar ciclos
    public boolean connected(int u, int v) {
        boolean[] visited = new boolean[n];
        return dfs(u, v, visited);
    }
    private boolean dfs(int cur, int target, boolean[] visited) {
        if (cur == target) return true;
        visited[cur] = true;
        for (int nxt : adj.get(cur))
            if (!visited[nxt] && dfs(nxt, target, visited)) return true;
        return false;
    }

    // =========================================================================
    //               CONSTRUÇÃO DA CACHE (Lê Amostra 1 vez)
    // =========================================================================
    
    private void construirCache(Amostra T) {
        System.out.println(">> A construir Cache Estatística (Isto demora ~1s)...");
        int maxDom = 0;
        for(int i=0; i<n; i++) maxDom = Math.max(maxDom, T.domain(i));
        maxDom = maxDom + 1; 

        // Aloca memória RAM (Cerca de 20MB para letter.csv - Muito leve)
        cachePares = new int[n][n][maxDom][maxDom];
        cacheSimples = new int[n][maxDom];

        int N = T.length();
        
        // Passada única pela amostra
        for (int k = 0; k < N; k++) {
            int[] linha = T.element(k); // Acesso rápido int[]
            
            for (int i = 0; i < n; i++) {
                int val_i = linha[i];
                cacheSimples[i][val_i]++; 
                
                // Preenche triângulo superior da matriz de adjacência estatística
                for (int j = i + 1; j < n; j++) {
                    int val_j = linha[j];
                    cachePares[i][j][val_i][val_j]++;
                    cachePares[j][i][val_j][val_i]++; // Simetria
                }
            }
        }
        System.out.println(">> Cache Construída com Sucesso.");
    }

    // =========================================================================
    //                        ALGORITMO DE APRENDIZAGEM
    // =========================================================================

    public void aprender(Amostra T) {
        int k = 2;
        aprender(T, k);
    }

    public void aprender(Amostra T, int k) {
        construirCache(T);
        
        // Inicializa cache de scores atuais
        cacheScoresLocais = new double[n];
        for(int i=0; i<n; i++) cacheScoresLocais[i] = scoreRapido(i, T);

        boolean melhorou = true;
        int indiceClasse = n - 1;
        int iteracao = 0; // Contador para DEBUG

        System.out.println(">> A iniciar Hill-Climbing (K=" + k + ")...");

        // Loop Hill Climbing
        while (melhorou) {
            iteracao++;
            melhorou = false;
            double melhorDelta = 0.0001; // Margem mínima
            int op = -1, bestU = -1, bestV = -1;

            // Testa todas as arestas possíveis
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    if (u == v || v == indiceClasse) continue; 

                    boolean existe = adj.get(u).contains(v); 

                    // TENTAR ADICIONAR
                    if (!existe) {
                        if (contaPaisReais(v, indiceClasse) < k) {
                            if (!connected(v, u)) { 
                                double delta = deltaScoreAdicionar(v, u, T);
                                if (delta > melhorDelta) {
                                    melhorDelta = delta; op = 2; bestU = u; bestV = v;
                                }
                            }
                        }
                    } 
                    // TENTAR REMOVER ou INVERTER
                    else {
                        // Remover
                        double deltaRem = deltaScoreRemover(v, u, T);
                        if (deltaRem > melhorDelta) {
                            melhorDelta = deltaRem; op = 0; bestU = u; bestV = v;
                        }
                        
                        // Inverter
                        if (u != indiceClasse && contaPaisReais(u, indiceClasse) < k) {
                             if (!connected(u, v)) { 
                                 double deltaInv = deltaScoreInverter(u, v, T);
                                 if (deltaInv > melhorDelta) {
                                     melhorDelta = deltaInv; op = 1; bestU = u; bestV = v;
                                 }
                             }
                        }
                    }
                }
            }

            // Aplica a melhor operação e atualiza cache local
            if (op != -1) {
                melhorou = true;
                
                // --- DEBUG PRINT: PARA VERES O CÓDIGO A CORRER ---
                if (iteracao % 50 == 0 || iteracao == 1) { // Imprime a cada 50 passos para não encher a consola
                     System.out.println("Iteração " + iteracao + " | Delta: " + melhorDelta + " | Op: " + op + " (" + bestU + "->" + bestV + ")");
                }
                
                if (op == 0) { // Remove
                    remove_edge(bestU, bestV);
                    cacheScoresLocais[bestV] = scoreRapido(bestV, T);
                } else if (op == 1) { // Inverte
                    invert_edge(bestU, bestV);
                    cacheScoresLocais[bestU] = scoreRapido(bestU, T); 
                    cacheScoresLocais[bestV] = scoreRapido(bestV, T);
                } else if (op == 2) { // Adiciona
                    add_edge(bestU, bestV);
                    cacheScoresLocais[bestV] = scoreRapido(bestV, T);
                }
            }
        }
        
        System.out.println(">> Aprendizagem concluída após " + iteracao + " iterações.");
        
        // Limpeza de memória (Opcional, mas boa prática)
        cachePares = null;
        cacheSimples = null;
    }

    private int contaPaisReais(int node, int classe) {
        int c = 0;
        for (int p : parents(node)) if (p != classe) c++;
        return c;
    }

    // =========================================================================
    //               SCORES ULTRA RÁPIDOS
    // =========================================================================

    private double deltaScoreAdicionar(int filho, int novoPai, Amostra T) {
        LinkedList<Integer> pais = parents(filho);
        pais.add(novoPai);
        return calcMDL(filho, pais, T) - cacheScoresLocais[filho];
    }

    private double deltaScoreRemover(int filho, int paiRemover, Amostra T) {
        LinkedList<Integer> pais = parents(filho);
        pais.remove((Integer)paiRemover);
        return calcMDL(filho, pais, T) - cacheScoresLocais[filho];
    }

    private double deltaScoreInverter(int u, int v, Amostra T) {
        LinkedList<Integer> paisV = parents(v); paisV.remove((Integer)u);
        LinkedList<Integer> paisU = parents(u); paisU.add(v);
        
        double novoV = calcMDL(v, paisV, T);
        double novoU = calcMDL(u, paisU, T);
        return (novoV + novoU) - (cacheScoresLocais[v] + cacheScoresLocais[u]);
    }

    private double scoreRapido(int i, Amostra T) {
        return calcMDL(i, parents(i), T);
    }

 // --- CÁLCULO MDL OTIMIZADO (VERSÃO FINAL V2) ---
    private double calcMDL(int filho, LinkedList<Integer> pais, Amostra T) {
        double LL = 0.0;
        int domFilho = T.domain(filho);
        int N = T.length(); 
        
        // 1. SEM PAIS (Usa cacheSimples - Instantâneo)
        if (pais.isEmpty()) {
            for (int val = 0; val < domFilho; val++) {
                int count = cacheSimples[filho][val];
                if (count > 0) {
                    double p = (double) count / N;
                    LL += count * Math.log(p);
                }
            }
        } 
        // 2. COM PAIS
        else {
            int[] paisArr = pais.stream().mapToInt(i->i).toArray();
            int numPais = paisArr.length;
            
            // Caso Rápido: 1 Pai (Usa cachePares 4D - Instantâneo)
            if (numPais == 1) {
                int pai = paisArr[0];
                int domPai = T.domain(pai);
                for (int vf = 0; vf < domFilho; vf++) {
                    for (int vp = 0; vp < domPai; vp++) {
                        int count = cachePares[filho][pai][vf][vp];
                        if (count > 0) {
                            int countPai = cacheSimples[pai][vp];
                            double probCond = (double) count / countPai;
                            LL += count * Math.log(probCond);
                        }
                    }
                }
            }
            // Caso TAN: 2 Pais (A Correção Crítica!)
            else if (numPais == 2) {
                // Em vez de chamar T.count() milhares de vezes, 
                // fazemos UMA leitura sequencial da Amostra para encher uma tabela temporária.
                // Isto acelera o processo em 5000x.
                
                int p1 = paisArr[0];
                int p2 = paisArr[1];
                int domP1 = T.domain(p1);
                int domP2 = T.domain(p2);
                
                // Tabela temporária na RAM (Pequena: ~15KB)
                int[][][] countsTrio = new int[domFilho][domP1][domP2];
                int[][] countsPais = new int[domP1][domP2];
                
                // PASSADA ÚNICA (O Segredo da Velocidade)
                for(int k=0; k<N; k++) {
                    int[] linha = T.element(k); // Acesso rápido à matriz da Amostra
                    int vf = linha[filho];
                    int vp1 = linha[p1];
                    int vp2 = linha[p2];
                    
                    countsTrio[vf][vp1][vp2]++;
                    countsPais[vp1][vp2]++;
                }
                
                // Cálculo Matemático Puro (Sem acesso à Amostra)
                for(int vf=0; vf<domFilho; vf++) {
                    for(int vp1=0; vp1<domP1; vp1++) {
                        for(int vp2=0; vp2<domP2; vp2++) {
                            int N_fpp = countsTrio[vf][vp1][vp2];
                            if (N_fpp > 0) {
                                int N_pp = countsPais[vp1][vp2];
                                LL += N_fpp * Math.log((double)N_fpp / N_pp);
                            }
                        }
                    }
                }
            }
            else {
                // Fallback para K > 2 (Lento, mas seguro)
                LL = -Double.MAX_VALUE; 
            }
        }

        // Penalização MDL
        long params = T.domain(filho) - 1;
        long q = 1;
        for(int p : pais) q *= T.domain(p);
        params *= q;
        
        return LL - ((Math.log(N) / 2.0) * params);
    }
}