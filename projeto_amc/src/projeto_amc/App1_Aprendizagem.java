package projeto_amc;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.Random;


//Aplicação 1: Lê a amostra, aprende a rede e grava-a


public class App1_Aprendizagem extends JFrame {

    private static final long serialVersionUID = 1L;

    private JComboBox<String> datasetCombo;
    private JTextField kField, restartsField;
    private JTextArea logArea;
    private JButton learnButton;
    private JProgressBar progressBar;
    private JPanel configPanel;
    
    // Cores para os estados
    private static final Color COLOR_YELLOW = new Color(255, 200, 0);
    private static final Color COLOR_GREEN = new Color(0, 180, 0);
    private static final Color COLOR_RED = new Color(200, 0, 0);

    private final String[] datasets = {
            "bcancer.csv", "diabetes.csv", "hepatitis.csv"
            , "thyroid.csv", "soybean-large.csv", "satimage.csv", "letter.csv"
    };

    public App1_Aprendizagem() {
        setTitle("Aplicação 1 - Aprendizagem");
        setSize(750, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Fonte maior para labels e campos
        Font largerFont = new Font("SansSerif", Font.PLAIN, 14);
        Font titleFont = new Font("SansSerif", Font.BOLD, 14);

        // Painel de configuração (sem o botão)
        configPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        updateConfigBorder(COLOR_YELLOW); // Começa amarelo
        
        JLabel datasetLabel = new JLabel("Dataset:");
        datasetLabel.setFont(largerFont);
        configPanel.add(datasetLabel);
        datasetCombo = new JComboBox<>(datasets);
        datasetCombo.setFont(largerFont);
        configPanel.add(datasetCombo);

        JLabel kLabel = new JLabel("Máximo de pais atributos (k):");
        kLabel.setFont(largerFont);
        configPanel.add(kLabel);
        kField = new JTextField("2", 5);
        kField.setFont(largerFont);
        configPanel.add(kField);

        JLabel restartsLabel = new JLabel("Número de restarts aleatórios:");
        restartsLabel.setFont(largerFont);
        configPanel.add(restartsLabel);
        restartsField = new JTextField("10", 5);
        restartsField.setFont(largerFont);
        configPanel.add(restartsField);

        // Área de Log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        logArea.setBackground(new Color(230, 245, 255)); // Azul muito claro
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(null, "Log da Execução",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION, titleFont));

        // Painel inferior com barra de progresso e botão
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Pronto");
        progressBar.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        learnButton = new JButton("Aprender e Gravar Rede (.bn)");
        learnButton.setBackground(Color.WHITE);
        learnButton.setForeground(Color.BLACK);
        learnButton.setFont(new Font("SansSerif", Font.BOLD, 14));

        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(learnButton, BorderLayout.SOUTH);

        add(configPanel, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        learnButton.addActionListener(e -> executeLearning());
        
        log("Aplicação pronta! Selecione um Dataset e preencha os restantes campos.");
    }
    
    private void updateConfigBorder(Color color) {
        configPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(color, 3),
                "Configuração",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 14),
                color));
    }

    private void executeLearning() {
        new Thread(() -> { // Interface gráfica continua responsiva;
            try {
                learnButton.setEnabled(false);
                SwingUtilities.invokeLater(() -> updateConfigBorder(COLOR_YELLOW));
                updateProgress(0, "A iniciar...");
                
                String dataset = (String) datasetCombo.getSelectedItem();
                
                // Validação e Ajuste de K (pode ser 0, 1 ou 2)
                int k = Integer.parseInt(kField.getText().trim());
                if (k < 0 || k > 2) {
                    k = 2;
                    SwingUtilities.invokeLater(() -> kField.setText("2")); 
                    log(">>> AVISO: K ajustado para 2 (deve ser 0, 1 ou 2).");
                }

                int restarts = Integer.parseInt(restartsField.getText().trim());
                if (restarts < 1) throw new IllegalArgumentException("Parâmetros inválidos.");

                updateProgress(10, "A carregar dados...");
                log("\n>>> A carregar dados: " + dataset);
                Amostra sample = new Amostra(dataset);
                log(">>> Dados carregados. Dimensões: " + sample.length() + " x " + sample.dim());

                updateProgress(20, "A criar grafo...");
                // 1. Criar grafo vazio
                Graphoo graph = new Graphoo(sample.dim());
                
                // 2. Executar o algoritmo de aprendizagem EXTERNO
                updateProgress(30, "A executar Hill Climbing...");
                log(">>> A executar Hill Climbing com " + restarts + " restarts...");
                learn(graph, sample, k, restarts);

                updateProgress(80, "A calcular score final...");
                log(">>> Score MDL Final: " + String.format("%.4f", graph.MDL(sample)));
                log(">>> Construindo BN e gravando...");
                
                updateProgress(90, "A gravar ficheiro...");
                BN network = new BN(graph, sample, 0.5);

                String bnFileName = dataset.replace(".csv", ".bn");
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(bnFileName))) {
                    oos.writeObject(network);
                }

                updateProgress(100, "Concluído!");
                SwingUtilities.invokeLater(() -> updateConfigBorder(COLOR_GREEN));
                log(">>> SUCESSO! Ficheiro criado: " + bnFileName);

            } catch (Exception ex) {
                updateProgress(0, "Erro!");
                SwingUtilities.invokeLater(() -> updateConfigBorder(COLOR_RED));
                log("ERRO: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                learnButton.setEnabled(true);
            }
        }).start();
    }
    
    private void updateProgress(int value, String text) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            progressBar.setString(text);
        });
    }

    // =========================================================================
    //        ALGORITMO DE APRENDIZAGEM (FORA DA GRAPHOO)
    // =========================================================================

    private void learn(Graphoo g, Amostra T, int k, int numStarts) {
        int n = T.dim();
        int classIndex = n - 1;

        // 1. Configurar Grafo Base (Classe é pai de todos)
        for (int i = 0; i < classIndex; i++) {
            g.add_edge(classIndex, i);
        }

        // Variáveis para guardar o melhor resultado
        Graphoo bestGraph = new Graphoo(g); // Usa o construtor de cópia
        double bestScore = bestGraph.MDL(T);

        // 2. Random Restarts (com progresso de 30% a 80%)
        for (int s = 0; s < numStarts; s++) {

            // Calcular progresso: de 30% a 80% baseado no número de restarts
            int progress = 30 + (int)((s + 1) * 50.0 / numStarts);
            updateProgress(progress, "Restart " + (s + 1) + "/" + numStarts);
            
            Graphoo candidate;
            if (s == 0) {
                // Primeira tentativa: começa do grafo base (vazio + classe)
                candidate = new Graphoo(g);
            } else {
                // Outras tentativas: começa de grafo aleatório
                candidate = generateRandomGraph(n, k, classIndex);
            }

            // Executar Hill Climbing neste candidato
            executeHillClimbing(candidate, T, k);

            // Verificar se é o melhor
            double currentScore = candidate.MDL(T);
            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestGraph = new Graphoo(candidate); // Guardar cópia do vencedor
                log("   > Novo melhor score encontrado: " + String.format("%.2f", bestScore));
            }
        }

        // 3. Copiar o melhor resultado para o grafo original 'g'
        // Como não podemos fazer "g = melhorGrafo", temos de copiar a estrutura
        // Assumindo que g está vazio ou sujo, limpamo-lo primeiro? 
        // A Graphoo não tem "clear", mas podemos reconstruir.
        
        // Estratégia de Cópia Manual (já que não podemos mudar Graphoo):
        // 1. Removemos todas as arestas existentes em G
        for(int i=0; i<n; i++) {
            // Copiar lista para evitar ConcurrentModificationException
            LinkedList<Integer> parents = new LinkedList<>(g.parents(i));
            for(Integer p : parents) g.remove_edge(p, i);
        }
        
        // 2. Adicionamos as arestas do melhorGrafo
        for(int i=0; i<n; i++) {
            // parents() retorna quem aponta para i. add_edge(pai, filho)
            for(Integer parent : bestGraph.parents(i)) {
                g.add_edge(parent, i);
            }
        }
    }

    // Cria grafos aleatórios obrigatóriamente acíclicos
    private Graphoo generateRandomGraph(int n, int k, int classIndex) {
        Graphoo g = new Graphoo(n);
        // Adicionar arestas da classe aos Xi's
        for (int i = 0; i < classIndex; i++) g.add_edge(classIndex, i);

        Random rand = new Random();
        int attempts = n * k; // Define quantas tentativas de adicionar arestas aleatórias

        for (int t = 0; t < attempts; t++) {
            int u = rand.nextInt(classIndex); // Pai (atributo)
            int v = rand.nextInt(classIndex); // Filho (atributo)

            if (u != v) {
                // Verifica se já existe
                if (!g.parents(v).contains(u)) {
                    // Verifica limite de pais (lembrando que parents() inclui a classe)
                    if (g.parents(v).size() < k + 1) { // k atributos + 1 classe
                        // Verifica ciclo
                        if (!g.connected(v, u)) { // Impede a criação de ciclos
                            g.add_edge(u, v);
                        }
                    }
                }
            }
        }
        return g;
    }

    private void executeHillClimbing(Graphoo g, Amostra T, int k) {
        int n = T.dim();
        int classIndex = n - 1;
        int maxParents = k + 1; // k + classe
        boolean improved = true;

        while (improved) {
            improved = false;
            double bestDelta = 0.0001; // Limiar mínimo de melhoria para aceitar uma operação no Hill Climbing
            int op = -1, bestU = -1, bestV = -1;

            // Testar todas as arestas possíveis
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    // Ignorar arestas da classe ou para a classe
                    if (u == v || u == classIndex || v == classIndex) continue;

                    boolean exists = g.parents(v).contains(u);

                    if (!exists) {
                        // Tentar ADICIONAR (op2)
                        if (g.parents(v).size() < maxParents && !g.connected(v, u)) {
                            double delta = g.MDLdelta(T, u, v, 2);
                            if (delta > bestDelta) {
                                bestDelta = delta; op = 2; bestU = u; bestV = v;
                            }
                        }
                    } else {
                        // Tentar REMOVER (op0)
                        double deltaRem = g.MDLdelta(T, u, v, 0);
                        if (deltaRem > bestDelta) {
                            bestDelta = deltaRem; op = 0; bestU = u; bestV = v;
                        }

                        // Tentar INVERTER (op1)
                        // Verifica se u pode receber v como pai sem criar ciclos
                        if (g.parents(u).size() < maxParents && !g.connected(u, v)) {
                            double deltaInv = g.MDLdelta(T, u, v, 1);
                            if (deltaInv > bestDelta) {
                                bestDelta = deltaInv; op = 1; bestU = u; bestV = v;
                            }
                        }
                    }
                }
            }

            // Aplicar melhor operação
            if (op != -1) {
                improved = true;
                if (op == 0) g.remove_edge(bestU, bestV);
                else if (op == 1) g.invert_edge(bestU, bestV);
                else if (op == 2) g.add_edge(bestU, bestV);
            }
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new App1_Aprendizagem().setVisible(true));
    }
}