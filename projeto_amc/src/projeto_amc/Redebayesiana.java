package projeto_amc;

import java.util.LinkedList;

public class Redebayesiana {

    private Grafosorientados G;
    private Amostra T;
    private double S; // Parâmetro de suavização (Pseudo-contagem), normalmente 0.5

    // Construtor
    // Recebe Grafo, Amostra e parâmetro de suavização
    public Redebayesiana(Grafosorientados g, Amostra t, double s) {
        this.G = g;
        this.T = t;
        this.S = s;
    }

    // Classifica um paciente (sem a classe) e devolve a classe mais provável
    public int classificar(int[] dadosPaciente) {
        int indiceClasse = T.dim() - 1;
        int melhorClasse = -1;
        double melhorProb = -1.0;

        int numClasses = T.domain(indiceClasse);

        // Cópia para testar diferentes valores da classe
        int[] casoTeste = dadosPaciente.clone();

        for (int c = 0; c < numClasses; c++) {
            casoTeste[indiceClasse] = c;

            // P(Sintomas, Classe = c)
            double p = prob(casoTeste);

            if (p > melhorProb) {
                melhorProb = p;
                melhorClasse = c;
            }
        }
        return melhorClasse;
    }

    // Calcula a probabilidade conjunta P(X1, ..., Xn)
    // P(X1...Xn) = ∏ P(Xi | Pais(Xi))
    public double prob(int[] evento) {
        double probabilidade = 1.0;
        int n = T.dim();

        for (int i = 0; i < n; i++) {
            probabilidade *= probabilidadeCondicional(i, evento);
        }
        return probabilidade;
    }

    // Calcula P(Xi = xi | Pais(Xi)) com suavização de Laplace/Dirichlet
    private double probabilidadeCondicional(int Xi, int[] evento) {
        LinkedList<Integer> pais = G.parents(Xi);

        int numPais = pais.size();

        // Xi + Pais
        int[] varsCond = new int[numPais + 1];
        int[] valsCond = new int[numPais + 1];

        // Apenas Pais
        int[] varsPais = new int[numPais];
        int[] valsPais = new int[numPais];

        for (int k = 0; k < numPais; k++) {
            int pai = pais.get(k);
            varsCond[k] = pai;
            valsCond[k] = evento[pai];
            varsPais[k] = pai;
            valsPais[k] = evento[pai];
        }

        varsCond[numPais] = Xi;
        valsCond[numPais] = evento[Xi];

        // Contagens na amostra
        double Ncond = T.count(varsCond, valsCond);
        double Npais = (numPais == 0)
                ? T.length()
                : T.count(varsPais, valsPais);

        int Di = T.domain(Xi);

        // (N_cond + S) / (N_pais + S * |Domínio|)
        return (Ncond + S) / (Npais + S * Di);
    }
}
