package projeto_amc;

import java.util.Arrays;
import java.util.Scanner;

public class Main_projeto {

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        // ================= SELEÇÃO DO DATASET =================
        String[] ficheirosDisponiveis = {
            "bcancer.csv",
            "diabetes.csv",
            "hepatitis.csv",
            "letter.csv",
            "satimage.csv",
            "soybean-large.csv",
            "thyroid.csv"
        };

        System.out.println("============================================");
        System.out.println("      ESCOLHA O DATASET PARA ANÁLISE");
        System.out.println("============================================");
        
        for (int i = 0; i < ficheirosDisponiveis.length; i++) {
            System.out.println("[" + (i + 1) + "] " + ficheirosDisponiveis[i]);
        }

        String ficheiro = "";
        while (true) {
            System.out.print("\nEscolha uma opção (1-" + ficheirosDisponiveis.length + "): ");
            if (input.hasNextInt()) {
                int escolha = input.nextInt();
                if (escolha >= 1 && escolha <= ficheirosDisponiveis.length) {
                    ficheiro = ficheirosDisponiveis[escolha - 1];
                    break;
                }
            } else {
                input.next(); // Limpar buffer de entrada inválida
            }
            System.out.println("Opção inválida. Tente novamente.");
        }

        System.out.println("\n>>> A carregar dados de: " + ficheiro);
        Amostra amostraCompleta = new Amostra(ficheiro);

        // Verificação de segurança
        if (amostraCompleta.length() == 0) {
            System.err.println("Erro: Ficheiro vazio ou não encontrado!");
            input.close();
            return;
        }

        System.out.println("Dados carregados com sucesso.");
        System.out.println("Total de casos na base de dados: " + amostraCompleta.length());
        int numVariaveis = amostraCompleta.dim();
        System.out.println("Número de variáveis: " + numVariaveis);

        // ================= PARTE 1: VALIDAÇÃO LEAVE-ONE-OUT =================
        System.out.println("\n=== INICIANDO VALIDAÇÃO LEAVE-ONE-OUT ===");
        System.out.println("Modo: Otimizado (Cache HashMap ativo na classe Amostra)");
        System.out.println("Aviso: Datasets grandes podem demorar algum tempo...");
        
        long inicio = System.currentTimeMillis();
        int acertos = 0;
        int total = amostraCompleta.length();

        for (int i = 0; i < total; i++) {
            Amostra treino = amostraCompleta.amostraSem(i);
            int[] teste = amostraCompleta.element(i);
            int classeReal = teste[teste.length - 1];

            // Aprender estrutura
            Grafosorientados grafo = new Grafosorientados(treino.dim());
            grafo.aprender(treino); 

            // Classificar
            Redebayesiana classificador = new Redebayesiana(grafo, treino, 0.5);

            int[] testeSemClasse = teste.clone();
            testeSemClasse[testeSemClasse.length - 1] = 0; 

            int previsao = classificador.classificar(testeSemClasse);

            if (previsao == classeReal) {
                acertos++;
            }

            // Feedback visual para o utilizador não pensar que o programa encravou
            if (i % 10 == 0) System.out.print(".");
            if (i % 100 == 0 && i > 0) System.out.println(" (" + i + " processados)");
        }

        long fim = System.currentTimeMillis();

        System.out.println("\n\n=== RELATÓRIO FINAL ===");
        System.out.println("Ficheiro analisado: " + ficheiro);
        System.out.println("Total de Casos Testados: " + total);
        System.out.println("Total de Acertos: " + acertos);
        double accuracy = (double) acertos / total * 100;
        System.out.printf("PRECISÃO (Accuracy): %.2f%%\n", accuracy);
        System.out.println("Tempo de execução: " + (fim - inicio) / 1000 + " segundos.");

        // ================= PARTE 2: VERIFICAR CASO EXISTENTE =================
        System.out.println("\n============================================");
        System.out.println("   CONSULTAR DIAGNÓSTICO DE PACIENTE EXISTENTE");
        System.out.println("============================================");

        Grafosorientados grafoFinal = null;
        Redebayesiana classificadorFinal = null;
        boolean modeloTreinado = false;

        while (true) {
            System.out.print("\nDeseja verificar um caso específico? (S/N): ");
            String resposta = input.next();

            if (resposta.equalsIgnoreCase("N")) {
                System.out.println("A encerrar o programa. Adeus!");
                break;
            } else if (!resposta.equalsIgnoreCase("S")) {
                System.out.println("Opção inválida.");
                continue;
            }

            // 1. Treinar o modelo final (uma única vez com TODOS os dados)
            if (!modeloTreinado) {
                System.out.println(">> A treinar o modelo final com a base de dados completa (" + ficheiro + ")...");
                grafoFinal = new Grafosorientados(numVariaveis);
                grafoFinal.aprender(amostraCompleta); 
                classificadorFinal = new Redebayesiana(grafoFinal, amostraCompleta, 0.5);
                modeloTreinado = true;
                System.out.println(">> Modelo pronto!");
            }

            // 2. Pedir o índice do paciente
            int maxIndice = total - 1;
            int indiceEscolhido = -1;
            
            while (true) {
                try {
                    System.out.print("Insira o ID do Paciente (0 a " + maxIndice + "): ");
                    if (input.hasNextInt()) {
                        indiceEscolhido = input.nextInt();
                        if (indiceEscolhido >= 0 && indiceEscolhido <= maxIndice) {
                            break;
                        } else {
                            System.out.println("Erro: Índice fora dos limites.");
                        }
                    } else {
                        System.out.println("Erro: Insira um número inteiro.");
                        input.next();
                    }
                } catch (Exception e) {
                    System.out.println("Erro inesperado na leitura.");
                    input.next(); 
                }
            }

            // 3. Obter os dados reais
            int[] dadosReais = amostraCompleta.element(indiceEscolhido);
            int classeReal = dadosReais[dadosReais.length - 1];
            
            // 4. Preparar para teste (esconder a classe)
            int[] dadosParaTeste = dadosReais.clone();
            dadosParaTeste[dadosParaTeste.length - 1] = 0; // Mascara a classe

            // 5. Classificar
            int previsao = classificadorFinal.classificar(dadosParaTeste);
            
            // 6. Mostrar resultados
            System.out.println("\n--- ANÁLISE DO PACIENTE #" + indiceEscolhido + " (" + ficheiro + ") ---");
            System.out.println("Dados completos: " + Arrays.toString(dadosReais));
            System.out.println("Diagnóstico Real (Ficheiro): " + classeReal);
            System.out.println("Diagnóstico Previsto (IA):   " + previsao);
            
            if (classeReal == previsao) {
                System.out.println("RESULTADO: SUCESSO (A previsão está correta!)");
            } else {
                System.out.println("RESULTADO: ERRO (A previsão falhou.)");
            }
        }
        
        input.close();
    }
}