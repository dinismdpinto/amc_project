package projeto_amc;

import java.util.LinkedList;

public class RedeBayes {

    Grafosorientados G;
    Amostra T;
    double S; // Pseudo-contagem (geralmente 0.5)

    /* ================= CONSTRUTOR ================= */
    public RedeBayes(Grafosorientados g, Amostra t, double s) {
        this.G = g;
        this.T = t;
        this.S = s;
    }

    /* ================= MÉTODO PROB ================= */
    // Calcula a probabilidade de um caso completo (ex: sintomas + diagnóstico)
    public double prob(int[] evento) {
        double probabilidade = 1.0;
        int n = T.dim(); 

        for (int i = 0; i < n; i++) {
            probabilidade *= probabilidadeCondicional(i, evento);
        }

        return probabilidade;
    }

    /* ================= MÉTODO CLASSIFICAR (NOVO) ================= */
    // Recebe um paciente (sintomas) e descobre a classe mais provável (0, 1, etc.)
    public int classificar(int[] dadosPaciente) {
        int indiceClasse = T.dim() - 1; // A classe é sempre a última coluna
        int melhorClasse = -1;
        double melhorProb = -1.0;

        int dominioClasse = T.domain(indiceClasse); // Quantas classes existem (ex: 2 para cancro)

        // Fazemos uma cópia dos dados para não estragar o original
        int[] casoTeste = dadosPaciente.clone();

        // Testamos todas as hipóteses: "E se for classe 0?", "E se for classe 1?"...
        for (int c = 0; c < dominioClasse; c++) {
            casoTeste[indiceClasse] = c; // Assume a classe 'c'
            
            double p = prob(casoTeste); // Vê a probabilidade dessa hipótese
            
            if (p > melhorProb) {
                melhorProb = p;
                melhorClasse = c;
            }
        }
        return melhorClasse; // Retorna a classe vencedora (ex: 1)
    }

    /* ================= MÉTODO AUXILIAR ================= */
    private double probabilidadeCondicional(int Xi, int[] evento) {
        LinkedList<Integer> pais = G.parents(Xi);
        
        int[] vars_cond = new int[pais.size() + 1];
        int[] vals_cond = new int[pais.size() + 1];
        int[] vars_pais = new int[pais.size()];
        int[] vals_pais = new int[pais.size()];

        for (int k = 0; k < pais.size(); k++) {
            int pai = pais.get(k);
            vars_cond[k] = pai;
            vals_cond[k] = evento[pai];
            vars_pais[k] = pai;
            vals_pais[k] = evento[pai];
        }
        
        vars_cond[pais.size()] = Xi;
        vals_cond[pais.size()] = evento[Xi];

        double N_cond = T.count(vars_cond, vals_cond);
        double N_pais = (pais.isEmpty()) ? T.length() : T.count(vars_pais, vals_pais);
        int Di = T.domain(Xi);

        return (N_cond + S) / (N_pais + S * Di);
    }
}