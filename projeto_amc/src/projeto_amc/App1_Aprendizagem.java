package projeto_amc;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.Random;

/**
 * Aplicação 1: Aprendizagem da Rede Bayesiana
 * Adaptação: O algoritmo de aprendizagem está implementado AQUI 
 * para não alterar a classe Graphoo.
 */
public class App1_Aprendizagem extends JFrame {

    private static final long serialVersionUID = 1L;

    private JComboBox<String> comboDatasets;
    private JTextField campoK, campoRestarts;
    private JTextArea areaLog;
    private JButton btnAprender;
    private JProgressBar barraProgresso;

    private final String[] datasets = {
            "bcancer.csv", "diabetes.csv", "hepatitis.csv",
            "parkisons.csv", "thyroid.csv", "soybean-large.csv", "satimage.csv", "letter.csv"
    };

    public App1_Aprendizagem() {
        setTitle("Aplicação 1 - Aprendizagem (Lógica Externa)");
        setSize(750, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Painel de configuração (sem o botão)
        JPanel painelConfig = new JPanel(new GridLayout(3, 2, 10, 10));
        painelConfig.setBorder(BorderFactory.createTitledBorder(null, "Configuração", 
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, 
                javax.swing.border.TitledBorder.DEFAULT_POSITION, 
                new Font("SansSerif", Font.BOLD, 14)));

        // Fonte maior para labels e campos
        Font fonteMaior = new Font("SansSerif", Font.PLAIN, 14);
        
        JLabel lblDataset = new JLabel("Dataset:");
        lblDataset.setFont(fonteMaior);
        painelConfig.add(lblDataset);
        comboDatasets = new JComboBox<>(datasets);
        comboDatasets.setFont(fonteMaior);
        painelConfig.add(comboDatasets);

        JLabel lblK = new JLabel("Máximo de pais atributos (k):");
        lblK.setFont(fonteMaior);
        painelConfig.add(lblK);
        campoK = new JTextField("2", 5);
        campoK.setFont(fonteMaior);
        painelConfig.add(campoK);

        JLabel lblRestarts = new JLabel("Número de restarts aleatórios:");
        lblRestarts.setFont(fonteMaior);
        painelConfig.add(lblRestarts);
        campoRestarts = new JTextField("10", 5);
        campoRestarts.setFont(fonteMaior);
        painelConfig.add(campoRestarts);

        // Área de Log (centro)
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log da Execução"));

        // Painel inferior com barra de progresso e botão
        JPanel painelInferior = new JPanel(new BorderLayout(10, 10));
        painelInferior.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        barraProgresso = new JProgressBar(0, 100);
        barraProgresso.setStringPainted(true);
        barraProgresso.setString("Pronto");
        barraProgresso.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        btnAprender = new JButton("Aprender e Gravar Rede (.bn)");
        btnAprender.setBackground(new Color(0, 128, 0));
        btnAprender.setForeground(Color.BLACK); // Letra preta
        btnAprender.setFont(new Font("SansSerif", Font.BOLD, 14));

        painelInferior.add(barraProgresso, BorderLayout.CENTER);
        painelInferior.add(btnAprender, BorderLayout.SOUTH);

        add(painelConfig, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        add(painelInferior, BorderLayout.SOUTH);

        btnAprender.addActionListener(e -> executarAprendizagem());
        
        log("Aplicação pronta. O algoritmo de aprendizagem correrá aqui localmente.");
    }

    private void executarAprendizagem() {
        new Thread(() -> {
            try {
                btnAprender.setEnabled(false);
                atualizarProgresso(0, "A iniciar...");
                
                String dataset = (String) comboDatasets.getSelectedItem();
                
                // Validação e Ajuste de K
                int kInput = Integer.parseInt(campoK.getText().trim());
                int k = kInput;
                if (k > 2) {
                    k = 2;
                    SwingUtilities.invokeLater(() -> campoK.setText("2")); 
                    log(">>> AVISO: K ajustado para 2 (Limite teórico).");
                }

                int restarts = Integer.parseInt(campoRestarts.getText().trim());
                if (k < 0 || restarts < 1) throw new IllegalArgumentException("Parâmetros inválidos.");

                atualizarProgresso(10, "A carregar dados...");
                log("\n>>> A carregar dados: " + dataset);
                Amostra amostra = new Amostra(dataset);
                log(">>> Dados carregados. Dimensões: " + amostra.length() + " x " + amostra.dim());

                atualizarProgresso(20, "A criar grafo...");
                // 1. Criar grafo vazio
                Graphoo grafo = new Graphoo(amostra.dim());
                
                // 2. Executar o algoritmo de aprendizagem EXTERNO
                atualizarProgresso(30, "A executar Hill Climbing...");
                log(">>> A executar Hill Climbing com " + restarts + " restarts...");
                aprenderExternamente(grafo, amostra, k, restarts);

                atualizarProgresso(80, "A calcular score final...");
                log(">>> Score MDL Final: " + String.format("%.4f", grafo.MDL(amostra)));
                log(">>> Construindo BN e gravando...");
                
                atualizarProgresso(90, "A gravar ficheiro...");
                BN rede = new BN(grafo, amostra, 0.5);

                String ficheiroBn = dataset.replace(".csv", ".bn");
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ficheiroBn))) {
                    oos.writeObject(rede);
                }

                atualizarProgresso(100, "Concluído!");
                log(">>> SUCESSO! Ficheiro criado: " + ficheiroBn);

            } catch (Exception ex) {
                atualizarProgresso(0, "Erro!");
                log("ERRO: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                btnAprender.setEnabled(true);
            }
        }).start();
    }
    
    private void atualizarProgresso(int valor, String texto) {
        SwingUtilities.invokeLater(() -> {
            barraProgresso.setValue(valor);
            barraProgresso.setString(texto);
        });
    }

    // =========================================================================
    //        ALGORITMO DE APRENDIZAGEM (FORA DA GRAPHOO)
    // =========================================================================

    private void aprenderExternamente(Graphoo g, Amostra T, int k, int numStarts) {
        int n = T.dim();
        int classe = n - 1;

        // 1. Configurar Grafo Base (Classe é pai de todos)
        for (int i = 0; i < classe; i++) {
            g.add_edge(classe, i);
        }

        // Variáveis para guardar o melhor resultado
        Graphoo melhorGrafo = new Graphoo(g); // Usa o construtor de cópia
        double melhorScore = melhorGrafo.MDL(T);

        // 2. Random Restarts (com progresso de 30% a 80%)
        for (int s = 0; s < numStarts; s++) {
            // Calcular progresso: de 30% a 80% baseado no número de restarts
            int progresso = 30 + (int)((s + 1) * 50.0 / numStarts);
            atualizarProgresso(progresso, "Restart " + (s + 1) + "/" + numStarts);
            
            Graphoo candidato;
            if (s == 0) {
                // Primeira tentativa: começa do grafo base (vazio + classe)
                candidato = new Graphoo(g);
            } else {
                // Outras tentativas: começa de grafo aleatório
                candidato = gerarGrafoAleatorio(n, k, classe);
            }

            // Executar Hill Climbing neste candidato
            executarHillClimbing(candidato, T, k);

            // Verificar se é o melhor
            double scoreAtual = candidato.MDL(T);
            if (scoreAtual > melhorScore) {
                melhorScore = scoreAtual;
                melhorGrafo = new Graphoo(candidato); // Guardar cópia do vencedor
                log("   > Novo melhor score encontrado: " + String.format("%.2f", melhorScore));
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
            LinkedList<Integer> pais = new LinkedList<>(g.parents(i));
            for(Integer p : pais) g.remove_edge(p, i);
        }
        
        // 2. Adicionamos as arestas do melhorGrafo
        for(int i=0; i<n; i++) {
            // parents() retorna quem aponta para i. add_edge(pai, filho)
            for(Integer pai : melhorGrafo.parents(i)) {
                g.add_edge(pai, i);
            }
        }
    }

    private Graphoo gerarGrafoAleatorio(int n, int k, int classe) {
        Graphoo g = new Graphoo(n);
        // Adicionar arestas fixas da classe
        for (int i = 0; i < classe; i++) g.add_edge(classe, i);

        Random rand = new Random();
        int tentativas = n * k; // Tenta adicionar algumas arestas extra

        for (int t = 0; t < tentativas; t++) {
            int u = rand.nextInt(classe); // Pai (atributo)
            int v = rand.nextInt(classe); // Filho (atributo)

            if (u != v) {
                // Verifica se já existe
                if (!g.parents(v).contains(u)) {
                    // Verifica limite K (lembrando que parents() inclui a classe)
                    if (g.parents(v).size() < k + 1) { // +1 da classe
                        // Verifica ciclo
                        if (!g.connected(v, u)) {
                            g.add_edge(u, v);
                        }
                    }
                }
            }
        }
        return g;
    }

    private void executarHillClimbing(Graphoo g, Amostra T, int k) {
        int n = T.dim();
        int classe = n - 1;
        int maxParents = k + 1; // Atributos + Classe
        boolean melhorou = true;

        while (melhorou) {
            melhorou = false;
            double melhorDelta = 0.0001;
            int op = -1, bestU = -1, bestV = -1;

            // Testar todas as arestas possíveis
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    // Ignorar arestas da classe ou para a classe
                    if (u == v || u == classe || v == classe) continue;

                    boolean existe = g.parents(v).contains(u);

                    if (!existe) {
                        // Tentar ADICIONAR (2)
                        if (g.parents(v).size() < maxParents && !g.connected(v, u)) {
                            double delta = g.MDLdelta(T, u, v, 2);
                            if (delta > melhorDelta) {
                                melhorDelta = delta; op = 2; bestU = u; bestV = v;
                            }
                        }
                    } else {
                        // Tentar REMOVER (0)
                        double deltaRem = g.MDLdelta(T, u, v, 0);
                        if (deltaRem > melhorDelta) {
                            melhorDelta = deltaRem; op = 0; bestU = u; bestV = v;
                        }

                        // Tentar INVERTER (1)
                        // Verifica se 'u' tem espaço para receber 'v' como pai
                        if (g.parents(u).size() < maxParents && !g.connected(u, v)) {
                            double deltaInv = g.MDLdelta(T, u, v, 1);
                            if (deltaInv > melhorDelta) {
                                melhorDelta = deltaInv; op = 1; bestU = u; bestV = v;
                            }
                        }
                    }
                }
            }

            // Aplicar melhor operação
            if (op != -1) {
                melhorou = true;
                if (op == 0) g.remove_edge(bestU, bestV);
                else if (op == 1) g.invert_edge(bestU, bestV);
                else if (op == 2) g.add_edge(bestU, bestV);
            }
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            areaLog.append(msg + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new App1_Aprendizagem().setVisible(true));
    }
}