package projeto_amc;

public class Main_projeto {

    public static void main(String[] args) {
        // ================= CONFIGURAÇÃO =================
        // 1. Escolhe o ficheiro
        String ficheiro = "bcancer.csv"; 
        
        // 2. Define os nomes das classes (0, 1, 2...)
        // Para o bcancer: 0 é Benigno, 1 é Maligno (geralmente)
        String[] etiquetas = { "Benigno", "Maligno" };
        
        // Se fores usar o thyroid.csv, comenta a linha de cima e usa:
        // String[] etiquetas = { "Normal", "Hipertiroidismo", "Hipotiroidismo" };
        // ================================================

        System.out.println(">>> A carregar " + ficheiro + "...");
        Amostra amostra = new Amostra(ficheiro); 
        
        if (amostra.length() == 0) {
            System.err.println("Erro: Amostra vazia.");
            return;
        }
        System.out.println("Amostra: " + amostra.length() + " casos, " + amostra.dim() + " variáveis.");

        // --- APRENDIZAGEM (HILL CLIMBER) ---
        Grafosorientados grafo = new Grafosorientados(amostra.dim());
        System.out.println(">>> A aprender estrutura da rede...");
        long startTime = System.currentTimeMillis();

        boolean melhorou = true;
        while (melhorou) {
            melhorou = false;
            double melhorDelta = 0.0;
            int op = -1, bestU = -1, bestV = -1;

            for (int u = 0; u < amostra.dim(); u++) {
                for (int v = 0; v < amostra.dim(); v++) {
                    if (u == v) continue;
                    boolean existe = grafo.parents(v).contains(u);
                    
                    if (!existe) { // Tentar ADICIONAR
                        double d = grafo.MDLdelta(amostra, u, v, 2);
                        if (d > melhorDelta) { melhorDelta = d; bestU = u; bestV = v; op = 2; }
                    } else { // Tentar REMOVER ou INVERTER
                        double dRem = grafo.MDLdelta(amostra, u, v, 0);
                        if (dRem > melhorDelta) { melhorDelta = dRem; bestU = u; bestV = v; op = 0; }
                        
                        double dInv = grafo.MDLdelta(amostra, u, v, 1);
                        if (dInv > melhorDelta) { melhorDelta = dInv; bestU = u; bestV = v; op = 1; }
                    }
                }
            }

            if (melhorDelta > 0.0001) {
                melhorou = true;
                if (op == 0) grafo.remove_edge(bestU, bestV);
                else if (op == 1) grafo.invert_edge(bestU, bestV);
                else if (op == 2) grafo.add_edge(bestU, bestV);
            }
        }
        long endTime = System.currentTimeMillis();

        // --- CLASSIFICAÇÃO ---
        RedeBayes redeFinal = new RedeBayes(grafo, amostra, 0.5);

        System.out.println("\n=== RESULTADOS DA APRENDIZAGEM ===");
        System.out.println("Tempo: " + (endTime - startTime) + "ms");
        System.out.println("Score MDL: " + grafo.MDL(amostra));
        
        // TESTE: Classificar o primeiro paciente da lista
        System.out.println("\n=== TESTE DE CLASSIFICAÇÃO (Paciente #1) ===");
        int[] paciente = amostra.element(0);
        int classeReal = paciente[paciente.length - 1]; // O último valor é a classe real
        
        // A rede tenta adivinhar
        int previsao = redeFinal.classificar(paciente);
        
        // Traduzir números para texto (ex: 0 -> "Benigno")
        String textoReal = (classeReal < etiquetas.length) ? etiquetas[classeReal] : "Classe " + classeReal;
        String textoPrev = (previsao < etiquetas.length) ? etiquetas[previsao] : "Classe " + previsao;

        System.out.println("Realidade: " + textoReal + " (" + classeReal + ")");
        System.out.println("Previsão:  " + textoPrev + " (" + previsao + ")");
        
        if (classeReal == previsao) {
            System.out.println("Resultado: A rede acertou.");
        } else {
            System.out.println("Resultado: A rede errou.");
        }
    }
}