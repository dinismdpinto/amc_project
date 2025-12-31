package projeto_amc;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;

/**
 * Rede Bayesiana (Classe BN)
 * Correção: Resolve o erro NotSerializableException do Graphoo
 * Otimização: Não grava a Amostra inteira no disco, apenas os domínios.
 */
public class BN implements Serializable {

    private static final long serialVersionUID = 1L;

    // "transient" significa: Java, ignora isto ao gravar. Eu trato disso manualmente.
    private transient Graphoo G;
    
    // Substituímos 'Amostra T' por arrays simples (mais leve e seguro)
    private int[] dominios; 
    private int dim; 

    private double[][] CPTs;
    private int[][] strides;
    private double[] probsClasse;
    private double S;

    /**
     * Construtor
     */
    public BN(Graphoo g, Amostra t, double s) {
        this.G = g;
        this.S = s;
        
        // Copiar metadados da Amostra para não depender do objeto T
        this.dim = t.dim();
        this.dominios = new int[dim];
        for (int i = 0; i < dim; i++) {
            this.dominios[i] = t.domain(i);
        }
        
        treinarOffline(t); // Passamos t apenas para treinar
    }

    // ====================== GETTERS PÚBLICOS ======================

    public int getDim() {
        return dim;
    }

    public int getDomain(int var) {
        return dominios[var];
    }

    // ====================== SERIALIZAÇÃO CUSTOMIZADA ======================
    // Estes métodos resolvem o problema do Graphoo não ser Serializable
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // 1. Grava tudo o que é normal (CPTs, dominios, etc.)
        out.defaultWriteObject();
        
        // 2. Grava manualmente a estrutura do Graphoo
        // Como não podemos gravar o objeto, gravamos as arestas.
        for (int i = 0; i < dim; i++) {
            LinkedList<Integer> pais = G.parents(i);
            out.writeInt(pais.size()); // Quantos pais tem o nó i?
            for (Integer p : pais) {
                out.writeInt(p);       // Quem são eles?
            }
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // 1. Lê tudo o que é normal
        in.defaultReadObject();
        
        // 2. Reconstrói o Graphoo manualmente
        this.G = new Graphoo(this.dim);
        
        for (int i = 0; i < this.dim; i++) {
            int numPais = in.readInt();
            for (int k = 0; k < numPais; k++) {
                int pai = in.readInt();
                this.G.add_edge(pai, i); // Adiciona a aresta (pai -> filho)
            }
        }
    }

    // ====================== MÉTODO PROB ======================

    public double prob(int[] caso) {
        double logProbTotal = 0.0;
        int indiceClasse = dim - 1;

        logProbTotal += probsClasse[caso[indiceClasse]];

        for (int i = 0; i < dim - 1; i++) {
            LinkedList<Integer> pais = G.parents(i);
            int index = calculaIndice(i, caso, pais);
            logProbTotal += CPTs[i][index];
        }

        return Math.exp(logProbTotal);
    }

    // ====================== TREINO ======================

    private void treinarOffline(Amostra T) {
        int classe = dim - 1;
        int domClasse = dominios[classe];

        // Priors da classe
        probsClasse = new double[domClasse];
        int[] countsClasse = new int[domClasse];
        for (int i = 0; i < T.length(); i++) {
            countsClasse[T.element(i)[classe]]++;
        }
        for (int c = 0; c < domClasse; c++) {
            double prob = (countsClasse[c] + S) / (T.length() + S * domClasse);
            probsClasse[c] = Math.log(prob);
        }

        // CPTs dos atributos
        CPTs = new double[dim - 1][];
        strides = new int[dim - 1][];

        for (int i = 0; i < dim - 1; i++) {
            LinkedList<Integer> pais = G.parents(i);
            int domFilho = dominios[i];

            int tamanhoCPT = domFilho;
            for (int p : pais) tamanhoCPT *= dominios[p];

            CPTs[i] = new double[tamanhoCPT];
            strides[i] = new int[pais.size() + 1];

            strides[i][0] = 1;
            int accum = domFilho;
            int pIdx = 1;
            for (int p : pais) {
                strides[i][pIdx++] = accum;
                accum *= dominios[p];
            }

            // Contagem
            int[] counts = new int[tamanhoCPT];
            for (int k = 0; k < T.length(); k++) {
                int[] linha = T.element(k);
                int index = calculaIndice(i, linha, pais);
                counts[index]++;
            }

            // Suavização e Log
            int configsPais = tamanhoCPT / domFilho;
            for (int conf = 0; conf < configsPais; conf++) {
                int base = conf * domFilho;
                double total = S * domFilho;
                for (int v = 0; v < domFilho; v++) total += counts[base + v];

                for (int v = 0; v < domFilho; v++) {
                    double probCond = (counts[base + v] + S) / total;
                    CPTs[i][base + v] = Math.log(probCond);
                }
            }
        }
    }

    private int calculaIndice(int varIndex, int[] valores, LinkedList<Integer> pais) {
        int idx = valores[varIndex];
        int pCount = 0;
        for (int pai : pais) {
            idx += valores[pai] * strides[varIndex][pCount + 1];
            pCount++;
        }
        return idx;
    }

    // ====================== CLASSIFICAÇÃO ======================

    public int classificar(int[] dadosPaciente) {
        int classeIdx = dim - 1;
        int domClasse = dominios[classeIdx];

        double melhorLogProb = Double.NEGATIVE_INFINITY;
        int melhorClasse = -1;

        for (int c = 0; c < domClasse; c++) {
            dadosPaciente[classeIdx] = c;
            double logProb = probsClasse[c];

            for (int i = 0; i < dim - 1; i++) {
                int idx = calculaIndice(i, dadosPaciente, G.parents(i));
                logProb += CPTs[i][idx];
            }

            if (logProb > melhorLogProb) {
                melhorLogProb = logProb;
                melhorClasse = c;
            }
        }
        return melhorClasse;
    }
}