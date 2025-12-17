package projeto_amc;

import java.util.LinkedList;
import java.util.Arrays;

public class Redebayesiana {
    Amostra amostra;
    Grafosorientados grafo;

    public Redebayesiana(Amostra amostra) {
        this.amostra = amostra;
        // Cria um grafo vazio com o número de variáveis da amostra
        this.grafo = new Grafosorientados(amostra.element());
    }

    // --- ALGORITMO DE APRENDIZAGEM (Hill Climbing) ---
    // Tenta adicionar arestas para melhorar a pontuação da rede
    public void learn() {
        boolean melhorou = true;
        int n = amostra.element();
        
        System.out.println("A iniciar aprendizagem (Hill Climbing)...");

        while (melhorou) {
            melhorou = false;
            double melhorScoreAtual = calcularScore(this.grafo);
            int melhorOrigem = -1;
            int melhorDestino = -1;
            String melhorOperacao = "NENHUMA";

            // Tenta todas as combinações possíveis de arestas
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) continue; // Não pode ligar a si próprio

                    // 1. Tentar ADICIONAR aresta (i -> j)
                    // Verifica se a aresta não existe e se não cria ciclo
                    if (!grafo.adj.get(i).contains(j)) {
                        grafo.add_edge(i, j); // Adiciona temporariamente
                        if (!grafo.hasCycle()) {
                            double novoScore = calcularScore(grafo);
                            if (novoScore > melhorScoreAtual) {
                                melhorScoreAtual = novoScore;
                                melhorOrigem = i;
                                melhorDestino = j;
                                melhorOperacao = "ADICIONAR";
                                melhorou = true;
                            }
                        }
                        grafo.remove_edge(i, j); // Remove para testar o próximo
                    }
                    
                    // 2. Tentar REMOVER aresta (se existir)
                    else {
                        grafo.remove_edge(i, j); // Remove temporariamente
                        double novoScore = calcularScore(grafo);
                        if (novoScore > melhorScoreAtual) {
                            melhorScoreAtual = novoScore;
                            melhorOrigem = i;
                            melhorDestino = j;
                            melhorOperacao = "REMOVER";
                            melhorou = true;
                        }
                        grafo.add_edge(i, j); // Põe de volta
                    }
                }
            }

            // Se encontrou uma melhoria, aplica a mudança permanentemente
            if (melhorou) {
                if (melhorOperacao.equals("ADICIONAR")) {
                    grafo.add_edge(melhorOrigem, melhorDestino);
                    System.out.println("Melhoria: Adicionada aresta " + melhorOrigem + " -> " + melhorDestino);
                } else if (melhorOperacao.equals("REMOVER")) {
                    grafo.remove_edge(melhorOrigem, melhorDestino);
                    System.out.println("Melhoria: Removida aresta " + melhorOrigem + " -> " + melhorDestino);
                }
            }
        }
        System.out.println("Aprendizagem concluída.");
    }

    // --- CÁLCULO DO SCORE (MDL / Log-Likelihood) ---
    // Calcula quão bem o grafo explica os dados
    public double calcularScore(Grafosorientados g) {
        double logLikelihood = 0;
        int n = amostra.element();

        // Para cada variável (nó)
        for (int i = 0; i < n; i++) {
            LinkedList<Integer> pais = g.parents(i);
            int[] vars = new int[pais.size() + 1];
            
            // Configura array de variáveis: [Pai1, Pai2, ..., Filho]
            for(int k=0; k<pais.size(); k++) vars[k] = pais.get(k);
            vars[pais.size()] = i; // O último é a própria variável

            // Chama função recursiva para somar probabilidades de todas as configurações
            logLikelihood += somarLogProb(vars, new int[vars.length], 0, i);
        }
        
        // Penalização MDL (opcional, mas recomendada para evitar grafos muito complexos)
        // MDL = LL - 0.5 * log(N) * NumParametros
        // Aqui usamos apenas LogLikelihood simples para garantir que funciona primeiro
        return logLikelihood;
    }

    // Função auxiliar recursiva para percorrer todas as combinações de valores dos pais
    private double somarLogProb(int[] vars, int[] vals, int index, int targetNodeIndex) {
        // Caso base: array de valores preenchido
        if (index == vars.length) {
            // Separa os valores dos pais e do filho
            int[] paisVals = Arrays.copyOf(vals, vals.length - 1);
            int[] paisVars = Arrays.copyOf(vars, vars.length - 1);
            
            // N_ijk: Quantas vezes esta configuração (pais + filho) acontece
            double N_ijk = amostra.count(vars, vals);
            
            // N_ij: Quantas vezes a configuração dos PAIS acontece
            double N_ij = amostra.count(paisVars, paisVals);

            if (N_ijk == 0) return 0; // log(0) é indefinido, assumimos 0
            
            // Fórmula: N_ijk * log(N_ijk / N_ij)
            return N_ijk * Math.log(N_ijk / N_ij);
        }

        double sum = 0;
        // Itera sobre o domínio da variável atual (vars[index])
        // Nota: Assumimos que domain() devolve o tamanho (ex: 2 para binário)
        int domainSize = amostra.domain(vars[index]); 
        
        for (int v = 0; v < domainSize; v++) {
            vals[index] = v;
            sum += somarLogProb(vars, vals, index + 1, targetNodeIndex);
        }
        return sum;
    }

    @Override
    public String toString() {
        return grafo.toString();
    }

    // --- MAIN PARA EXECUTAR O PROJETO ---
    public static void main(String[] args) {
        // 1. Carregar Amostra
        System.out.println(">>> A carregar dados...");
        Amostra amostra = new Amostra("bcancer.csv");
        
        if(amostra.length() == 0) {
            System.err.println("Erro: Amostra vazia. Verifique o ficheiro.");
            return;
        }

        // 2. Criar Rede Bayesiana
        Redebayesiana rede = new Redebayesiana(amostra);
        
        // 3. Aprender a Estrutura (Hill Climbing)
        long startTime = System.currentTimeMillis();
        rede.learn();
        long endTime = System.currentTimeMillis();
        
        // 4. Mostrar Resultados
        System.out.println("\n>>> Grafo Final Aprendido:");
        System.out.println(rede);
        System.out.println("Tempo de execução: " + (endTime - startTime) + "ms");
    }
}