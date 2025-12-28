package projeto_amc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

/**
 * Classe que representa Grafos Orientados Acíclicos para aprendizagem de estrutura
 * de Redes Bayesianas com limite k no grau de entrada (excluindo a classe).
 * Cumpre todos os requisitos da secção 3.2 do enunciado (pág. 27).
 */
public class Grafosorientados implements Serializable {

    private static final long serialVersionUID = 1L;

    int n; // número total de nós (atributos + classe)
    ArrayList<LinkedList<Integer>> adj; // lista de adjacência: adj[u] contém filhos de u

    // Caches por instância (transient para não gravar no disco ao serializar a BN)
    private transient int[][][][] cachePares;
    private transient int[][] cacheSimples;
    private transient double[] cacheScoresLocais;

    /**
     * Método exigido pelo enunciado (pág. 27): construtor "grafoo"
     */
    public static Grafosorientados grafoo(int n) {
        return new Grafosorientados(n);
    }

    /**
     * Construtor privado: cria grafo com n nós e adiciona arestas fixas C → Xi
     */
    private Grafosorientados(int n) {
        this.n = n;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adj.add(new LinkedList<>());

        int indiceClasse = n - 1;
        // Regra obrigatória (pág. 12): classe é pai de todos os atributos
        for (int i = 0; i < n - 1; i++) {
            adj.get(indiceClasse).add(i);
        }
    }

    /**
     * Clone profundo do grafo (usado nos random restarts)
     */
    public Grafosorientados clone() {
        Grafosorientados copia = new Grafosorientados(this.n);
        // Limpar arestas padrão da classe e copiar exatamente o estado atual
        int classe = n - 1;
        for (int i = 0; i < n - 1; i++) copia.adj.get(classe).remove((Integer) i);

        for (int u = 0; u < n; u++) {
            copia.adj.get(u).clear();
            copia.adj.get(u).addAll(this.adj.get(u));
        }
        return copia;
    }

    // ====================== OPERAÇÕES SOBRE ARESTAS ======================

    public void add_edge(int u, int v) {
        if (!adj.get(u).contains(v)) adj.get(u).add(v);
    }

    public void remove_edge(int u, int v) {
        // Proteção: nunca remover arestas da classe
        if (u == n - 1) return;
        adj.get(u).remove((Integer) v);
    }

    public void invert_edge(int u, int v) {
        // Proteção: nunca mexer em arestas que envolvam a classe
        if (u == n - 1 || v == n - 1) return;
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
        return dfs(u, v, visited);
    }

    private boolean dfs(int cur, int target, boolean[] visited) {
        if (cur == target) return true;
        visited[cur] = true;
        for (int nxt : adj.get(cur))
            if (!visited[nxt] && dfs(nxt, target, visited)) return true;
        return false;
    }

    private int contaPaisAtributos(int node, int classe) {
        int c = 0;
        for (int p : parents(node)) if (p != classe) c++;
        return c;
    }

    // ====================== CACHE ======================

    private void construirCache(Amostra T) {
        if (cacheSimples != null) return; // já construída

        int maxDom = 0;
        for (int i = 0; i < n; i++) maxDom = Math.max(maxDom, T.domain(i));
        maxDom++;

        cachePares = new int[n][n][maxDom][maxDom];
        cacheSimples = new int[n][maxDom];

        int N = T.length();
        for (int k = 0; k < N; k++) {
            int[] linha = T.element(k);
            for (int i = 0; i < n; i++) {
                int val_i = linha[i];
                cacheSimples[i][val_i]++;
                for (int j = i + 1; j < n; j++) {
                    int val_j = linha[j];
                    cachePares[i][j][val_i][val_j]++;
                    cachePares[j][i][val_j][val_i]++;
                }
            }
        }
    }

    // ====================== MÉTODOS OBRIGATÓRIOS (pág. 27) ======================

    /**
     * MDL total do grafo (soma dos scores MDL locais de todos os nós, incluindo a classe)
     */
    public double MDL(Amostra T) {
        construirCache(T);
        double total = 0.0;
        for (int i = 0; i < n; i++) {
            total += calcMDL(i, parents(i), T);
        }
        return total;
    }

    /**
     * Variação de MDL para uma operação específica
     * operacao: 0 = remover, 1 = inverter, 2 = adicionar
     */
    public double MDLdelta(Amostra T, int u, int v, int operacao) {
        construirCache(T);
        if (cacheScoresLocais == null) {
            cacheScoresLocais = new double[n];
            for (int i = 0; i < n; i++) cacheScoresLocais[i] = calcMDL(i, parents(i), T);
        }

        if (operacao == 0) return deltaScoreRemover(v, u, T);
        if (operacao == 1) return deltaScoreInverter(u, v, T);
        if (operacao == 2) return deltaScoreAdicionar(v, u, T);
        return 0.0;
    }

    // ====================== APRENDIZAGEM COM RANDOM RESTARTS ======================

    /**
     * Aprendizagem principal com múltiplos restarts (obrigatório no enunciado pág. 21)
     * Primeiro restart: grafo vazio (só arestas da classe)
     * Restantes: grafos aleatórios respeitando k e ausência de ciclos
     */
    public void aprender(Amostra T, int k, int numStarts) {
        construirCache(T);

        // Primeiro: grafo atual (começa vazio exceto arestas da classe)
        this.hillClimb(T, k);
        double melhorScore = MDL(T);
        Grafosorientados melhorGrafo = this.clone();

        Random rand = new Random();
        int classe = n - 1;

        for (int s = 1; s < numStarts; s++) {
            Grafosorientados candidato = this.clone(); // parte do grafo vazio + classe

            // Adicionar arestas aleatórias (respeitando k e sem ciclos)
            int tentativas = n * 10; // número suficiente de tentativas
            for (int t = 0; t < tentativas; t++) {
                int u = rand.nextInt(n - 1); // origem nunca é classe
                int v = rand.nextInt(n - 1);
                if (u != v && !candidato.adj.get(u).contains(v)) {
                    if (candidato.contaPaisAtributos(v, classe) < k && !candidato.connected(v, u)) {
                        candidato.add_edge(u, v);
                    }
                }
            }

            candidato.cachePares = this.cachePares;     // partilha cache (leitura apenas)
            candidato.cacheSimples = this.cacheSimples;
            candidato.cacheScoresLocais = null;

            candidato.hillClimb(T, k);

            double scoreCand = candidato.MDL(T);
            if (scoreCand > melhorScore) {
                melhorScore = scoreCand;
                melhorGrafo = candidato.clone();
            }
        }

        // Aplicar o melhor grafo encontrado
        this.adj = melhorGrafo.adj;

        // Limpeza de memória
        cachePares = null;
        cacheSimples = null;
        cacheScoresLocais = null;
    }

    public void aprender(Amostra T, int k) {
        aprender(T, k, 1); // compatibilidade antiga
    }

    public void aprender(Amostra T) {
        aprender(T, 2, 5); // default: k=2, 5 restarts (recomendado)
    }

    // ====================== GREEDY HILL CLIMBING ======================

    private void hillClimb(Amostra T, int k) {
        cacheScoresLocais = new double[n];
        for (int i = 0; i < n; i++) cacheScoresLocais[i] = calcMDL(i, parents(i), T);

        boolean melhorou = true;
        int classe = n - 1;

        while (melhorou) {
            melhorou = false;
            double melhorDelta = 0.0001;
            int op = -1, bestU = -1, bestV = -1;

            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    if (u == v || v == classe || u == classe) continue;

                    boolean existe = adj.get(u).contains(v);

                    if (!existe) {
                        if (contaPaisAtributos(v, classe) < k && !connected(v, u)) {
                            double delta = deltaScoreAdicionar(v, u, T);
                            if (delta > melhorDelta) {
                                melhorDelta = delta; op = 2; bestU = u; bestV = v;
                            }
                        }
                    } else {
                        double deltaRem = deltaScoreRemover(v, u, T);
                        if (deltaRem > melhorDelta) {
                            melhorDelta = deltaRem; op = 0; bestU = u; bestV = v;
                        }

                        if (contaPaisAtributos(u, classe) < k && !connected(u, v)) {
                            double deltaInv = deltaScoreInverter(u, v, T);
                            if (deltaInv > melhorDelta) {
                                melhorDelta = deltaInv; op = 1; bestU = u; bestV = v;
                            }
                        }
                    }
                }
            }

            if (op != -1) {
                melhorou = true;
                if (op == 0) {
                    remove_edge(bestU, bestV);
                    cacheScoresLocais[bestV] = calcMDL(bestV, parents(bestV), T);
                } else if (op == 1) {
                    invert_edge(bestU, bestV);
                    cacheScoresLocais[bestU] = calcMDL(bestU, parents(bestU), T);
                    cacheScoresLocais[bestV] = calcMDL(bestV, parents(bestV), T);
                } else if (op == 2) {
                    add_edge(bestU, bestV);
                    cacheScoresLocais[bestV] = calcMDL(bestV, parents(bestV), T);
                }
            }
        }
    }

    // ====================== DELTAS E SCORE RÁPIDO ======================

    private double deltaScoreAdicionar(int filho, int novoPai, Amostra T) {
        LinkedList<Integer> pais = parents(filho);
        pais.add(novoPai);
        return calcMDL(filho, pais, T) - cacheScoresLocais[filho];
    }

    private double deltaScoreRemover(int filho, int paiRemover, Amostra T) {
        LinkedList<Integer> pais = parents(filho);
        pais.remove((Integer) paiRemover);
        return calcMDL(filho, pais, T) - cacheScoresLocais[filho];
    }

    private double deltaScoreInverter(int u, int v, Amostra T) {
        LinkedList<Integer> paisV = parents(v); paisV.remove((Integer) u);
        LinkedList<Integer> paisU = parents(u); paisU.add(v);
        return (calcMDL(v, paisV, T) + calcMDL(u, paisU, T))
                - (cacheScoresLocais[v] + cacheScoresLocais[u]);
    }

    // ====================== CÁLCULO MDL (base 2, até 3 pais) ======================

    private double calcMDL(int filho, LinkedList<Integer> pais, Amostra T) {
        double LL = 0.0;
        double log2Div = Math.log(2.0);
        int domFilho = T.domain(filho);
        int N = T.length();

        int[] paisArr = pais.stream().mapToInt(i -> i).toArray();
        int numPais = paisArr.length;

        if (numPais == 1) { // Só classe → usa cachePares
            int pai = paisArr[0];
            int domPai = T.domain(pai);
            for (int vf = 0; vf < domFilho; vf++) {
                for (int vp = 0; vp < domPai; vp++) {
                    int count = cachePares[filho][pai][vf][vp];
                    if (count > 0) {
                        double prob = (double) count / cacheSimples[pai][vp];
                        LL += count * (Math.log(prob) / log2Div);
                    }
                }
            }
        } else if (numPais == 2) { // Classe + 1 atributo
            int p1 = paisArr[0], p2 = paisArr[1];
            int d1 = T.domain(p1), d2 = T.domain(p2);
            int[][][] trio = new int[domFilho][d1][d2];
            int[][] paisCount = new int[d1][d2];

            for (int k = 0; k < N; k++) {
                int[] l = T.element(k);
                trio[l[filho]][l[p1]][l[p2]]++;
                paisCount[l[p1]][l[p2]]++;
            }

            for (int vf = 0; vf < domFilho; vf++)
                for (int v1 = 0; v1 < d1; v1++)
                    for (int v2 = 0; v2 < d2; v2++) {
                        int njk = trio[vf][v1][v2];
                        if (njk > 0) {
                            double prob = (double) njk / paisCount[v1][v2];
                            LL += njk * (Math.log(prob) / log2Div);
                        }
                    }
        } else if (numPais == 3) { // Classe + 2 atributos
            int p1 = paisArr[0], p2 = paisArr[1], p3 = paisArr[2];
            int d1 = T.domain(p1), d2 = T.domain(p2), d3 = T.domain(p3);

            int[][][][] quart = new int[domFilho][d1][d2][d3];
            int[][][] paisCount = new int[d1][d2][d3];

            for (int k = 0; k < N; k++) {
                int[] l = T.element(k);
                quart[l[filho]][l[p1]][l[p2]][l[p3]]++;
                paisCount[l[p1]][l[p2]][l[p3]]++;
            }

            for (int vf = 0; vf < domFilho; vf++)
                for (int v1 = 0; v1 < d1; v1++)
                    for (int v2 = 0; v2 < d2; v2++)
                        for (int v3 = 0; v3 < d3; v3++) {
                            int njk = quart[vf][v1][v2][v3];
                            if (njk > 0 && paisCount[v1][v2][v3] > 0) {
                                double prob = (double) njk / paisCount[v1][v2][v3];
                                LL += njk * (Math.log(prob) / log2Div);
                            }
                        }
        } else {
            // Caso improvável (k excessivo ou erro)
            return -Double.MAX_VALUE;
        }

        // Penalização MDL
        long params = domFilho - 1;
        long q = 1;
        for (int p : paisArr) q *= T.domain(p);
        params *= q;

        double penal = (Math.log(N) / log2Div / 2.0) * params;
        return LL - penal;
    }
}