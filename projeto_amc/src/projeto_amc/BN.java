package projeto_amc;

//package projeto2025;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BN implements Serializable {

    private static final long serialVersionUID = 1L;

    private Graphoo graph;  
    private Amostra dataset; 
    
    private double[][] cpts;      // Tabela de Probabilidades Condicionais
    private int[][] strides;      // Encontra a linha certa na tabela
    private double[] classProbs;  // Probabilidade de cada Classe 
    private double S;             // O valor 'S' (suavização) para evitar zeros

    
    // Retorna a Rede de Bayes com as distribuições DFO amortizadas com pseudo-contagens
    public BN(Graphoo g, Amostra data, double s) {
        this.graph = g;
        this.dataset = data;
        this.S = s; // Garante que nenhuma probabilidade é nula (S = 0.5)
        
        trainOffline();
    }
    
    private void trainOffline() {
        int n = dataset.dim();          // Número total de colunas/ variáveis
        int classIndex = n - 1;         
        int classDomain = dataset.domain(classIndex); 
        
        classProbs = new double[classDomain];
        int[] classCounts = new int[classDomain];

        // Conta quantos casos se encaixam em cada caso do dominio da classe
        for (int i = 0; i < dataset.length(); i++) {
            int val = dataset.element(i)[classIndex]; // guarda em val o valor do elemento i da classe
            classCounts[val]++; 
        }

        // Transforma contagens em Log-Probabilidades 
        for (int c = 0; c < classDomain; c++) {
            double prob = (classCounts[c] + S) / (dataset.length() + S * classDomain); // Fórmula slide 18
            
            // A soma de logaritmos (ao invés do produto de probabilidades) evita erros de precisão dos resultados
            classProbs[c] = Math.log(prob);
        }

        // Cálculo das tabelas (CPTs) 
        cpts = new double[n - 1][];    // Uma tabela para cada parâmetro
        strides = new int[n - 1][];    // Uma ajuda de navegação para cada parâmetro

        for (int i = 0; i < n - 1; i++) { 
            // O classificador exige P(Atributo | Pais, Classe).
            LinkedList<Integer> graphParents = graph.parents(i);
            List<Integer> effectiveParents = new ArrayList<>(graphParents);
            effectiveParents.add(classIndex); 

            int childDomain = dataset.domain(i); // Domínio de um atributo específico

            // Cálculo do tamanho da tabela de probabilidades
            // Tamanho = (Valores do Filho) * (Valores do Pai 1) * (Valores do Pai 2)...
            int tableSize = childDomain;
            for (int p : effectiveParents) {
                tableSize *= dataset.domain(p);
            }

            cpts[i] = new double[tableSize];
            strides[i] = new int[effectiveParents.size() + 1];

            // Permite a navegação na tabela
            // Indica onde guardar os dados na lista.
            strides[i][0] = 1;
            int accum = childDomain;
            int pIdx = 1;
            for (int p : effectiveParents) {
                strides[i][pIdx++] = accum;
                accum *= dataset.domain(p);
            }

            // Conta ocorrências nos dados e encontra o local correto na tabela para guardar essa informação
            int[] counts = new int[tableSize];
            for (int k = 0; k < dataset.length(); k++) {
                int[] row = dataset.element(k);
                // Encontra a posição correta na tabela para esta linha de dados
                int index = computeIndex(i, row, effectiveParents);
                counts[index]++; // Incrementa 1 unidade ao valor associado a essa posição
            }

            // Converte as contagens em Probabilidades (com suavização S)
            int parentConfigs = tableSize / childDomain; // Nr de combinações de pais (incluindo classe) existentes
            
            for (int conf = 0; conf < parentConfigs; conf++) {
                int baseIndex = conf * childDomain;
                
                // Descobre o total de vezes que esta combinação de pais aconteceu
                double total = S * childDomain;
                for (int v = 0; v < childDomain; v++) {
                    total += counts[baseIndex + v];
                }

                // Calcula a probabilidade final e guarda o Logaritmo
                for (int v = 0; v < childDomain; v++) {
                    double probVal = (counts[baseIndex + v] + S) / total; // Fórmula slide 18
                    cpts[i][baseIndex + v] = Math.log(probVal);
                }
            }
        }
    }

    // Converte coordenadas multidimensionais numa Posição Linear.
    // Tranforma uma combinação de atributos num número único
    private int computeIndex(int varIndex, int[] values, List<Integer> parents) {
        int idx = values[varIndex]; // Valor associado ao atributo (variável) em questão
        int pCount = 0;
        for (int parent : parents) { // Percorre cada pai da lista dos pais 
            idx += values[parent] * strides[varIndex][pCount + 1]; // Quantas linhas da tabela correpondem a uma mudança unitária desse pai
            pCount++;
        }
        return idx;  // Nr do local da tabela onde se encontra a probabilidade 
    }

   
    // Calcula a probabilidade conjunta de um vetor completo (Atributos + Classe).
    public double prob(int[] instance) { // Recebe uma linha de dados completa (instância)
        double logTotal = 0.0;
        int classIndex = dataset.dim() - 1;

        // Probabilidade da Classe (P(C))
        logTotal += classProbs[instance[classIndex]];

        // 2. Somamos as probabilidades de cada atributo 
        for (int i = 0; i < dataset.dim() - 1; i++) {
        	
            // Pais efetivos: grafo + Classe
            LinkedList<Integer> graphParents = graph.parents(i);
            List<Integer> effectiveParents = new ArrayList<>(graphParents);
            effectiveParents.add(classIndex); // Adiciona a classe aos pais
            
            // Vai buscar a probabilidade da instancia e soma ao valor existente de logTotal 
            int index = computeIndex(i, instance, effectiveParents);
            logTotal += cpts[i][index];
        }

        // Reverte-se o logaritmo para obter a probabilidade real (0 a 1)
        return Math.exp(logTotal);
    }

    
    // Classifica: recebe os atributos e decide qual é a classe mais provável.
    public int classify(int[] inputVector) {
        int classIndex = dataset.dim() - 1;
        int numClasses = dataset.domain(classIndex);
        
        // Cópia dos dados originais
        int[] testInstance = inputVector.clone();

        double bestProb = Double.NEGATIVE_INFINITY; // Começa muito baixo (-Infinito)
        int bestClass = -1;

        // Testa-se a probabilidade da instância pertencer a cada classe consoante os atributos apresentados
        for (int c = 0; c < numClasses; c++) {
            testInstance[classIndex] = c; // Atribui-se uma instância a cada classe
            
            // Pergunta à rede: "Qual a probabilidade disto?"
            double currentProb = prob(testInstance);
            
            // Se esta probabilidade for maior que a melhor, guarda-se essa classe: melhor classificação
            if (currentProb > bestProb) {
                bestProb = currentProb;
                bestClass = c;
            }
        }
        // Devolve a classe vencedora
        return bestClass;
    }
    
    
    
    // Permite aceder ao tamanho e domínio sem mexer na lista 
    public int getDim() {
        return dataset.dim();
    }

    public int getDomain(int index) {
        return dataset.domain(index);
    }
}