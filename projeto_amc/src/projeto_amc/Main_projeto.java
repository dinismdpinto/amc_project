package projeto_amc;

import java.util.Scanner;

public class Main_projeto {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // ================= CONFIGURAÇÃO AUTOMÁTICA =================
        // Se Linhas * Colunas > h, usamos K-Fold. Senão, usamos LOO.
        long H_COMPLEXIDADE = 50000; 
        
        // Número de Folds padrão (KOHAVI, Ron (1995) "A Study of Cross-Validation and Bootstrap 
        //for Accuracy Estimation and Model Selection")
        int K_FOLD_VALUE = 10; 
        // ===========================================================

        String[] ficheiros = {
            "bcancer.csv", "diabetes.csv", "hepatitis.csv", 
            "thyroid.csv", "soybean-large.csv", "satimage.csv", "letter.csv"
        };

        System.out.println("=== CLASSIFICADOR AUTOMÁTICO ===");
        System.out.println("Escolha o dataset:");
        for (int i = 0; i < ficheiros.length; i++) {
            System.out.println("[" + (i + 1) + "] " + ficheiros[i]);
        }

        int escolha;
        try {
            System.out.print("Opção: ");
            escolha = scanner.nextInt() - 1;
        } catch (Exception e) {
            scanner.close();
            return;
        }

        if (escolha < 0 || escolha >= ficheiros.length) {
            scanner.close();
            return;
        }

        String ficheiro = ficheiros[escolha];

        // 1. CARREGAR AMOSTRA
        System.out.println("\n>>> A carregar " + ficheiro + "...");
        Amostra amostra = new Amostra(ficheiro);
        if (amostra.length() == 0) {
            scanner.close();
            return;
        }

        // 2. CÁLCULO DA COMPLEXIDADE
        long complexidade = (long) amostra.length() * amostra.dim();
        System.out.println("Dimensões: " + amostra.length() + " casos x " + amostra.dim() + " variáveis.");
        System.out.println("Complexidade (N*M): " + complexidade);

        // 3. DEFINIR LIMITE DE PAIS (k)
        // Para datasets muito complexos (letter/satimage), forçamos k=1 (TAN) para não bloquear.
        // Para os outros, tentamos k=2.
        int kPais = (complexidade > 100000) ? 1 : 2;
        System.out.println("Grau de pais escolhido: k=" + kPais +
                (kPais == 1 ? " (TAN - Rápido)" : " (K-DB - Preciso)"));

        // 4. DECISÃO DA ESTRATÉGIA DE TESTE
        if (complexidade < H_COMPLEXIDADE) {
            System.out.println("\n[DECISÃO] Dataset Leve -> A aplicar LEAVE-ONE-OUT...");
            executarLeaveOneOut(amostra, kPais);
        } else {
            System.out.println("\n[DECISÃO] Dataset Pesado -> A aplicar " + K_FOLD_VALUE + "-FOLD CROSS VALIDATION...");
            executarKFold(amostra, kPais, K_FOLD_VALUE);
        }

        scanner.close();
    }

    // -------------------------------------------------------------------------
    // ESTRATÉGIA 1: K-FOLD CROSS VALIDATION (RÁPIDO)
    // -------------------------------------------------------------------------
    public static void executarKFold(Amostra amostra, int kPais, int numFolds) {
        long inicio = System.currentTimeMillis();
        int totalAcertos = 0;
        int totalTestados = 0;
        int tamanhoFold = amostra.length() / numFolds;

        for (int fold = 0; fold < numFolds; fold++) {
            System.out.print("Fold " + (fold + 1) + "/" + numFolds + "... ");

            int inicioTeste = fold * tamanhoFold;
            int fimTeste = (fold == numFolds - 1) ? amostra.length() : inicioTeste + tamanhoFold;

            // Criar Treino (Copia tudo MENOS a fatia de teste)
            Amostra treino = new Amostra();
            // (Truque para copiar metadados do domínio)
            if (amostra.length() > 0) {
                treino.add(amostra.element(0));
                treino = treino.amostraSem(0);
            }

            for (int i = 0; i < amostra.length(); i++) {
                if (i < inicioTeste || i >= fimTeste) {
                    treino.add(amostra.element(i));
                }
            }

            // Aprender
            Grafosorientados grafo = new Grafosorientados(treino.dim());
            grafo.aprender(treino, kPais);

            // Classificar
            Redebayesiana rede = new Redebayesiana(grafo, treino, 0.5);
            int acertosFold = 0;
            int countFold = 0;

            for (int i = inicioTeste; i < fimTeste; i++) {
                int[] casoTeste = amostra.element(i);
                int real = casoTeste[casoTeste.length - 1];

                int previsao = rede.classificar(casoTeste);
                if (previsao == real) acertosFold++;
                countFold++;
            }

            totalAcertos += acertosFold;
            totalTestados += countFold;
            System.out.println("Acertos: " + acertosFold + "/" + countFold);
        }

        mostrarResultados(totalAcertos, totalTestados, inicio);
    }

    // -------------------------------------------------------------------------
    // ESTRATÉGIA 2: LEAVE-ONE-OUT (EXAUSTIVO)
    // -------------------------------------------------------------------------
    public static void executarLeaveOneOut(Amostra amostra, int kPais) {
        long inicio = System.currentTimeMillis();
        int acertos = 0;
        int total = amostra.length();

        for (int i = 0; i < total; i++) {
            Amostra treino = amostra.amostraSem(i);
            int[] teste = amostra.element(i);
            int real = teste[teste.length - 1];

            Grafosorientados grafo = new Grafosorientados(treino.dim());
            grafo.aprender(treino, kPais);

            Redebayesiana rede = new Redebayesiana(grafo, treino, 0.5);
            int previsao = rede.classificar(teste);

            if (previsao == real) acertos++;

            // Feedback visual a cada 5% de progresso
            if (i % Math.max(1, total / 20) == 0) System.out.print(".");
        }

        mostrarResultados(acertos, total, inicio);
    }

    private static void mostrarResultados(int acertos, int total, long inicio) {
        System.out.println("\n=== RESULTADOS FINAIS ===");
        System.out.println("Total Testado: " + total);
        System.out.println("Acertos: " + acertos);
        System.out.printf("PRECISÃO (Accuracy): %.2f%%\n", (double) acertos / total * 100);
        System.out.println("Tempo Total: " +
                (System.currentTimeMillis() - inicio) / 1000 + "s");
    }
}
