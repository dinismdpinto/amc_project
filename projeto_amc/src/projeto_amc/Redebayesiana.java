package projeto_amc;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Rede Bayesiana com getters públicos para dim() e domain()
 * Necessário para a App2_Classificacao poder gerar os inputs corretamente
 */
public class Redebayesiana implements Serializable {

    private static final long serialVersionUID = 1L;

    private Grafosorientados G;
    private Amostra T;                // Guarda os domínios
    private double[][] CPTs;
    private int[][] strides;
    private double[] probsClasse;
    private double S;

    public Redebayesiana(Grafosorientados g, Amostra t, double s) {
        this.G = g;
        this.T = t;
        this.S = s;
        treinarOffline(s);
    }

    // ====================== GETTERS PÚBLICOS (OBRIGATÓRIOS PARA App2) ======================

    /**
     * Retorna o número total de variáveis (atributos + classe)
     */
    public int getDim() {
        return T.dim();
    }

    /**
     * Retorna o domínio (número de valores possíveis + 1) de uma variável
     */
    public int getDomain(int var) {
        return T.domain(var);
    }

    // ====================== MÉTODO PROB ======================

    public double prob(int[] caso) {
        double logProbTotal = 0.0;
        int indiceClasse = T.dim() - 1;

        logProbTotal += probsClasse[caso[indiceClasse]];

        for (int i = 0; i < T.dim() - 1; i++) {
            LinkedList<Integer> pais = G.parents(i);
            int index = calculaIndice(i, caso, pais);
            logProbTotal += CPTs[i][index];
        }

        return Math.exp(logProbTotal);
    }

    // ====================== TREINO ======================

    private void treinarOffline(double S) {
        int n = T.dim();
        int classe = n - 1;
        int domClasse = T.domain(classe);

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
        CPTs = new double[n - 1][];
        strides = new int[n - 1][];

        for (int i = 0; i < n - 1; i++) {
            LinkedList<Integer> pais = G.parents(i);
            int domFilho = T.domain(i);

            int tamanhoCPT = domFilho;
            for (int p : pais) tamanhoCPT *= T.domain(p);

            CPTs[i] = new double[tamanhoCPT];
            strides[i] = new int[pais.size() + 1];

            strides[i][0] = 1;
            int accum = domFilho;
            int pIdx = 1;
            for (int p : pais) {
                strides[i][pIdx++] = accum;
                accum *= T.domain(p);
            }

            int[] counts = new int[tamanhoCPT];
            for (int k = 0; k < T.length(); k++) {
                int[] linha = T.element(k);
                int index = calculaIndice(i, linha, pais);
                counts[index]++;
            }

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
        int classeIdx = T.dim() - 1;
        int domClasse = T.domain(classeIdx);

        double melhorLogProb = Double.NEGATIVE_INFINITY;
        int melhorClasse = -1;

        for (int c = 0; c < domClasse; c++) {
            dadosPaciente[classeIdx] = c;

            double logProb = probsClasse[c];

            for (int i = 0; i < T.dim() - 1; i++) {
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