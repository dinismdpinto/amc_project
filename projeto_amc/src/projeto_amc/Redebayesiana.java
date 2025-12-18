package projeto_amc;

import java.util.LinkedList;

public class Redebayesiana {

    Grafosorientados G;
    Amostra T;
    double S; // Parâmetro de suavização (Pseudo-contagem), normalmente 0.5

    public RedeBayes(Grafosorientados g, Amostra t, double s) {
        this.G = g;
        this.T = t;
        this.S = s;
    }

    // --- MÉTODO CLASSIFICAR ---
    // Recebe os dados de um paciente (sem a classe) e descobre a classe mais provável.
    public int classificar(int[] dadosPaciente) {
        int indiceClasse = T.dim() - 1;
        int melhorClasse = -1;
        double melhorProb = -1.0;

        int numClasses = T.domain(indiceClasse);
        
        // Faz uma cópia dos dados para podermos testar diferentes classes
        int[] casoTeste = dadosPaciente.clone();

        // Para cada classe possível (0, 1, ...):
        for (int c = 0; c < numClasses; c++) {
            casoTeste[indiceClasse] = c; // "E se a classe for c?"

            // Calcula a probabilidade conjunta P(Sintomas, Classe=c)
            double p = prob(casoTeste);

            // Guarda a classe que deu a maior probabilidade
            if (p > melhorProb) {
                melhorProb = p;
                melhorClasse = c;
            }
        }
        return melhorClasse;
    }

    // Calcula a probabilidade de um evento completo usando a Regra da Cadeia
    // P(X1...Xn) = Prod P(Xi | Pais(Xi))
    public double prob(int[] evento) {
        double probabilidade = 1.0;
        int n = T.dim(); 

        for (int i = 0; i < n; i++) {
            probabilidade *= probabilidadeCondicional(i, evento);
        }
        return probabilidade;
    }

    // Calcula P(Xi = x | Pais = p) com Suavização de Laplace/Dirichlet
    private double probabilidadeCondicional(int Xi, int[] evento) {
        LinkedList<Integer> pais = G.parents(Xi);
        
        // Constrói os arrays para contar as ocorrências nos dados
        int[] vars_cond = new int[pais.size() + 1]; // Xi + Pais
        int[] vals_cond = new int[pais.size() + 1];
        int[] vars_pais = new int[pais.size()];     // Apenas Pais
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

        // Contagens na amostra
        double N_cond = T.count(vars_cond, vals_cond); // Quantas vezes Xi e Pais aparecem assim
        double N_pais = (pais.isEmpty()) ? T.length() : T.count(vars_pais, vals_pais); // Quantas vezes os Pais aparecem assim
        
        int Di = T.domain(Xi); // Tamanho do domínio de Xi

        // Fórmula da Probabilidade com Suavização 'S'
        // (N_cond + S) / (N_pais + S * |Dominio|)
        return (N_cond + S) / (N_pais + S * Di);
    }
}