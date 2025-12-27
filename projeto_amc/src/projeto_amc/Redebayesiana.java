package projeto_amc;

import java.util.LinkedList;

public class Redebayesiana {

    private Grafosorientados G;
    private Amostra T;
    
    // --- CPTs ACHATADAS (FLATTENED) ---
    // Em vez de matrizes complexas, guardamos um array simples de doubles para cada variável.
    // O índice é calculado com base nos valores dos pais.
    // CPTs[variavel][indiceCalculado]
    private double[][] CPTs; 
    
    // Guardamos os "pesos" (strides) para calcular o índice rapidamente
    private int[][] strides; 
    
    private double[] probsClasse;

    public Redebayesiana(Grafosorientados g, Amostra t, double s) {
        this.G = g;
        this.T = t;
        treinarOffline(s);
    }

    private void treinarOffline(double S) {
        int n = T.dim();
        int indiceClasse = n - 1;
        
        // 1. Probabilidade a priori da Classe P(C)
        int domClasse = T.domain(indiceClasse);
        probsClasse = new double[domClasse];
        int[] countsClasse = new int[domClasse];
        
        for (int i = 0; i < T.length(); i++) {
            int c = T.element(i)[indiceClasse];
            if (c < domClasse) countsClasse[c]++;
        }
        
        for (int c = 0; c < domClasse; c++) {
            probsClasse[c] = Math.log((countsClasse[c] + S) / (T.length() + S * domClasse));
        }

        // 2. Preparar Estruturas das CPTs
        CPTs = new double[n][];
        strides = new int[n][];

        // Alocar memória e calcular strides (multiplicadores)
        for (int i = 0; i < n; i++) {
            if (i == indiceClasse) continue;

            LinkedList<Integer> pais = G.parents(i);
            int tamanhoTabela = T.domain(i);
            
            // Lógica de "Pesos" para converter N pais num só índice inteiro
            // Ex: Indice = ValFilho + ValPai1*DomFilho + ValPai2*DomFilho*DomPai1 ...
            strides[i] = new int[pais.size() + 1];
            strides[i][0] = 1; 
            
            int acumulador = T.domain(i);
            for (int p = 0; p < pais.size(); p++) {
                strides[i][p+1] = acumulador;
                acumulador *= T.domain(pais.get(p));
            }
            
            CPTs[i] = new double[acumulador]; // Tamanho total necessário
        }

        // 3. Preencher CPTs (Passada ÚNICA pela Amostra = Velocidade Extrema)
        // Usamos arrays temporários para contagem para não ter de criar objetos
        int[][] contagens = new int[n][];
        for(int i=0; i<n; i++) {
            if (i != indiceClasse) contagens[i] = new int[CPTs[i].length];
        }

        int N = T.length();
        for (int k = 0; k < N; k++) {
            int[] linha = T.element(k); // Acesso direto rápido

            for (int i = 0; i < n; i++) {
                if (i == indiceClasse) continue;

                // Calcular índice linear baseado nos valores da linha atual
                int index = calculaIndice(i, linha, G.parents(i));
                contagens[i][index]++;
            }
        }

        // 4. Calcular Log-Probabilidades (Suavização de Laplace)
        for (int i = 0; i < n; i++) {
            if (i == indiceClasse) continue;

            int domFilho = T.domain(i);
            int totalConfigsPais = CPTs[i].length / domFilho;

            for (int configP = 0; configP < totalConfigsPais; configP++) {
                // Calcular total para esta configuração de pais (Denominador)
                int totalP = 0;
                int baseIdx = configP * domFilho; // Começo do bloco no array achatado
                
                // Nota: Devido à forma como os strides funcionam, os valores do filho
                // são os bits menos significativos, por isso estão contíguos ou em padrão.
                // Mas para simplificar a lógica de soma, vamos somar manualmente:
                
                // Na verdade, é mais simples somar os counts reais:
                for(int v = 0; v < domFilho; v++) {
                    totalP += contagens[i][baseIdx + v * strides[i][0]]; 
                    // Nota: a minha lógica de strides acima pôs o filho no stride[0]=1.
                    // Logo o bloco de um pai específico não é contíguo simples se tivermos mais pais.
                    // CORREÇÃO: Vamos somar corretamente usando o array de contagens.
                }

                // Mas espera, iterar o array flat é confuso para normalizar.
                // Truque: Vamos iterar linearmente o array de contagens e normalizar
                // assumindo que sabemos qual é o pai? Não.
                
                // Vamos usar a lógica reversa simples:
                // Prob(Filho=v | Pais)
                // Precisamos da soma N(Pais).
                // Como isto é "treino offline" e só acontece 1 vez, podemos ser menos eficientes aqui
                // para garantir que a matemática bate certo.
            }
            
            // REFAZENDO O PASSO 4 DE FORMA SEGURA:
            // Vamos iterar todas as células. Para cada célula, descobrimos quem são os pais,
            // e somamos o denominador.
            // Para ser rápido: pré-calcular denominadores é chato com array flat.
            // Vamos usar a Amostra.count? Não, lento.
            
            // Solução Elegante:
            // O array flat tem tamanho [DomPai2 * DomPai1 * DomFilho]
            // O "bloco" de probabilidades soma 1 para cada configuração de pais.
            // Como o 'Filho' tem stride 1, os valores do filho estão juntos?
            // Sim: Indice = vFilho + vPai * DomFilho.
            // Logo, a cada 'DomFilho' posições, temos uma distribuição completa.
            
            for (int base = 0; base < CPTs[i].length; base += domFilho) {
                // Dentro deste bloco, os Pais são fixos, só muda o Filho.
                int totalDestePai = 0;
                for (int v = 0; v < domFilho; v++) {
                    totalDestePai += contagens[i][base + v];
                }
                
                // Aplicar log prob
                for (int v = 0; v < domFilho; v++) {
                    double num = contagens[i][base + v] + S;
                    double den = totalDestePai + (S * domFilho);
                    CPTs[i][base + v] = Math.log(num / den);
                }
            }
        }
    }

    // Helper ultra-rápido para calcular posição no array
    private int calculaIndice(int varIndex, int[] valoresAtuais, LinkedList<Integer> pais) {
        int idx = valoresAtuais[varIndex]; // Stride[0] é sempre 1 (peso do filho)
        
        int pCount = 0;
        for (int pai : pais) {
            // idx += ValorDoPai * PesoDestePai
            idx += valoresAtuais[pai] * strides[varIndex][pCount + 1];
            pCount++;
        }
        return idx;
    }

    public int classificar(int[] dadosPaciente) {
        int indiceClasse = T.dim() - 1;
        int numClasses = T.domain(indiceClasse);
        
        double melhorLogProb = Double.NEGATIVE_INFINITY;
        int melhorClasse = -1;

        // Testar cada classe possível
        for (int c = 0; c < numClasses; c++) {
            double logProbAtual = probsClasse[c];
            
            // Injetamos a classe hipotética nos dados do paciente
            dadosPaciente[indiceClasse] = c; // MUITO IMPORTANTE: O grafo usa isto se a classe for pai!

            for (int i = 0; i < T.dim(); i++) {
                if (i == indiceClasse) continue;

                // Calcular índice condicional (funciona para 0, 1, 2... pais)
                // Nota: calculaIndice vai ler dadosPaciente[pai], e se o pai for a classe,
                // vai ler o 'c' que acabámos de definir. Perfeito.
                int index = calculaIndice(i, dadosPaciente, G.parents(i));
                
                logProbAtual += CPTs[i][index];
            }

            if (logProbAtual > melhorLogProb) {
                melhorLogProb = logProbAtual;
                melhorClasse = c;
            }
        }
        return melhorClasse;
    }
}