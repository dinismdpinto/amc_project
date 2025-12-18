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
        // O enunciado exige isto para avaliar a qualidade do classificador.
        // O algoritmo vai correr N vezes (onde N é o nº de pacientes).
        
        System.out.println("\n=== INICIANDO VALIDAÇÃO LEAVE-ONE-OUT ===");
        System.out.println("Aviso: Como treinamos a rede do zero para cada paciente,");
        System.out.println("este processo pode demorar alguns minutos...");
        
        long inicio = System.currentTimeMillis();
        int acertos = 0;
        int total = amostraCompleta.length();

        // 1. O GRANDE CICLO
        // Para cada linha 'i' do ficheiro:
        for (int i = 0; i < total; i++) {
            
            // A. PREPARAR DADOS (DIVIDIR EM TREINO E TESTE)
            // O paciente 'i' sai da sala (Teste)
            // Todos os outros ficam para ensinar o computador (Treino)
            Amostra treino = amostraCompleta.amostraSem(i);
            int[] teste = amostraCompleta.element(i);
            
            // O último valor da linha é a resposta correta (a Classe Real)
            int classeReal = teste[teste.length - 1]; 

            // B. APRENDIZAGEM (HILL CLIMBER)
            // Criamos um grafo vazio e mandamos aprender com os dados de TREINO
            Grafosorientados grafo = new Grafosorientados(treino.dim());
            
            // Esta função executa todo aquele 'while(melhorou)' que tinhas no main antigo
            grafo.aprender(treino); 

            // C. CLASSIFICAÇÃO
            // Criamos o classificador com o grafo que acabámos de aprender
            // S = 0.5 (Pseudo-contagem padrão)
            RedeBayes classificador = new RedeBayes(grafo, treino, 0.5); 
            
            // Pedimos para adivinhar a classe do paciente de teste
            int previsao = classificador.classificar(teste);

            // D. COMPARAÇÃO
            if (previsao == classeReal) {
                acertos++; // Boa! A rede acertou.
            }

            // E. FEEDBACK VISUAL
            // Imprime um ponto a cada 10 casos para saberes que o programa não bloqueou
            if (i % 10 == 0) System.out.print(".");
            // A cada 100 casos, muda de linha e mostra quantos já foram
            if (i % 100 == 0 && i > 0) System.out.println(" (" + i + " processados)");
        }
        
        long fim = System.currentTimeMillis();
        
        // ================= RESULTADOS FINAIS =================
        System.out.println("\n\n=== RELATÓRIO FINAL ===");
        System.out.println("Ficheiro analisado: " + ficheiro);
        System.out.println("Total de Casos Testados: " + total);
        System.out.println("Total de Acertos: " + acertos);
        System.out.println("Total de Erros: " + (total - acertos));
        
        // Cálculo da percentagem de precisão
        double accuracy = (double) acertos / total * 100;
        System.out.printf("PRECISÃO (Accuracy): %.2f%%\n", accuracy);
        
        long tempoTotal = (fim - inicio) / 1000;
        System.out.println("Tempo de execução: " + tempoTotal + " segundos.");
    }
}