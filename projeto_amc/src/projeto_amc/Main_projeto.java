package projeto_amc;

public class Main_projeto {

    public static void main(String[] args) {
        // ================= CONFIGURAÇÃO =================
        String ficheiro = "bcancer.csv";
        // Podes mudar para "thyroid.csv", "diabetes.csv", etc.

        System.out.println(">>> A carregar dados de: " + ficheiro);
        Amostra amostraCompleta = new Amostra(ficheiro);

        // Verificação de segurança
        if (amostraCompleta.length() == 0) {
            System.err.println("Erro: Ficheiro vazio ou não encontrado!");
            return;
        }

        System.out.println("Dados carregados com sucesso.");
        System.out.println("Total de casos na base de dados: " + amostraCompleta.length());
        System.out.println("Número de variáveis: " + amostraCompleta.dim());

        // ================= VALIDAÇÃO LEAVE-ONE-OUT =================
        System.out.println("\n=== INICIANDO VALIDAÇÃO LEAVE-ONE-OUT ===");
        System.out.println("Aviso: Como treinamos a rede do zero para cada paciente,");
        System.out.println("este processo pode demorar alguns minutos...");

        long inicio = System.currentTimeMillis();
        int acertos = 0;
        int total = amostraCompleta.length();

        // Loop principal do Leave-One-Out
        for (int i = 0; i < total; i++) {

            // A. PREPARAR DADOS (TREINO / TESTE)
            Amostra treino = amostraCompleta.amostraSem(i);
            int[] teste = amostraCompleta.element(i);

            int classeReal = teste[teste.length - 1];

            // B. APRENDIZAGEM DA ESTRUTURA (HILL CLIMBING)
            Grafosorientados grafo = new Grafosorientados(treino.dim());
            grafo.aprender(treino);

            // C. CLASSIFICAÇÃO BAYESIANA
            Redebayesiana classificador = new Redebayesiana(grafo, treino, 0.5);

            // Remove a classe real antes de classificar
            int[] testeSemClasse = teste.clone();
            testeSemClasse[testeSemClasse.length - 1] = 0;

            int previsao = classificador.classificar(testeSemClasse);

            // D. COMPARAÇÃO
            if (previsao == classeReal) {
                acertos++;
            }

            // E. FEEDBACK VISUAL
            if (i % 10 == 0) System.out.print(".");
            if (i % 100 == 0 && i > 0)
                System.out.println(" (" + i + " processados)");
        }

        long fim = System.currentTimeMillis();

        // ================= RESULTADOS FINAIS =================
        System.out.println("\n\n=== RELATÓRIO FINAL ===");
        System.out.println("Ficheiro analisado: " + ficheiro);
        System.out.println("Total de Casos Testados: " + total);
        System.out.println("Total de Acertos: " + acertos);
        System.out.println("Total de Erros: " + (total - acertos));

        double accuracy = (double) acertos / total * 100;
        System.out.printf("PRECISÃO (Accuracy): %.2f%%\n", accuracy);

        long tempoTotal = (fim - inicio) / 1000;
        System.out.println("Tempo de execução: " + tempoTotal + " segundos.");
    }
}