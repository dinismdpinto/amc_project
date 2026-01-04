package projeto_amc;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays; 

/**
 * Aplicação 2: Classificação e Diagnóstico
 * Requisito: Interface sequencial, texto preto, controlo pelo utilizador.
 * Com sistema de feedback visual (bordas coloridas) para indicar estados.
 */
public class App2_Classificacao extends JFrame {
    
    private static final long serialVersionUID = 1L;

    private JButton loadButton, classifyButton, changeFileButton, clearDataButton;
    private JLabel loadedFileLabel;
    private JTextArea logArea;
    
    // A nossa classe BN
    private BN loadedNetwork;
    
    private ArrayList<JTextField> manualInputs = new ArrayList<>();
    private JPanel inputsPanel;
    
    // Painéis para controlar as bordas
    private JPanel topPanel;
    private JPanel centerWrapperPanel;
    
    // Cores para os estados
    private static final Color COLOR_YELLOW = new Color(255, 200, 0);
    private static final Color COLOR_GREEN = new Color(0, 180, 0);
    private static final Color COLOR_RED = new Color(200, 0, 0);
    private static final Color COLOR_WHITE = Color.WHITE;
    
    // Fontes
    private Font defaultFont;
    private Font boldFont;
    private Font titleFont;

    public App2_Classificacao() {
        setTitle("Aplicação 2: Classificação (BN)");
        setSize(800, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrar no ecrã
        setLayout(new BorderLayout(10, 10));

        // Fonte consistente para toda a aplicação
        defaultFont = new Font("SansSerif", Font.PLAIN, 14);
        boldFont = new Font("SansSerif", Font.BOLD, 14);
        titleFont = new Font("SansSerif", Font.BOLD, 14);

        // =================================================================
        // PAINEL SUPERIOR: Passo 1 (Carregar)
        // =================================================================
        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        updateStep1Border(COLOR_YELLOW); // Começa amarelo

        loadButton = new JButton("Carregar Rede (.bn)");
        loadButton.setForeground(Color.BLACK);
        loadButton.setBackground(new Color(220, 220, 220));
        loadButton.setFont(boldFont);
        
        loadedFileLabel = new JLabel("Nenhum ficheiro carregado");
        loadedFileLabel.setFont(defaultFont);
        loadedFileLabel.setForeground(Color.GRAY);
        
        topPanel.add(loadButton);
        topPanel.add(loadedFileLabel);

        // =================================================================
        // PAINEL CENTRAL: Passo 2 (Inputs)
        // =================================================================
        centerWrapperPanel = new JPanel(new BorderLayout());
        updateStep2Border(COLOR_WHITE); // Começa branco
        
        inputsPanel = new JPanel(new GridLayout(0, 4, 10, 10));
        
        // Wrapper para alinhar à esquerda
        JPanel leftWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftWrapper.add(inputsPanel);
        
        centerWrapperPanel.add(new JScrollPane(leftWrapper), BorderLayout.CENTER);

        // =================================================================
        // PAINEL INFERIOR: Log + Botões
        // =================================================================
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        
        // Área de Log com fundo azul bebé
        logArea = new JTextArea(10, 40); 
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        logArea.setBackground(new Color(230, 245, 255)); // Azul muito claro
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(null, "Resultados e Log",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, titleFont));
        
        // Painel de botões (abaixo do log)
        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        // Botão Limpar Dados
        clearDataButton = new JButton("Limpar Dados");
        clearDataButton.setForeground(Color.BLACK);
        clearDataButton.setBackground(Color.WHITE);
        clearDataButton.setFont(boldFont);
        clearDataButton.setEnabled(false);
        
        // Botão Mudar Ficheiro
        changeFileButton = new JButton("Mudar Ficheiro");
        changeFileButton.setForeground(Color.BLACK);
        changeFileButton.setBackground(Color.WHITE);
        changeFileButton.setFont(boldFont);
        changeFileButton.setEnabled(false);
        
        // Botão Classificar
        classifyButton = new JButton("Classificar");
        classifyButton.setForeground(Color.BLACK);
        classifyButton.setBackground(Color.WHITE);
        classifyButton.setFont(boldFont);
        classifyButton.setEnabled(false);
        
        actionButtonsPanel.add(clearDataButton);
        actionButtonsPanel.add(changeFileButton);
        actionButtonsPanel.add(classifyButton);
        
        bottomPanel.add(logScrollPane, BorderLayout.CENTER);
        bottomPanel.add(actionButtonsPanel, BorderLayout.SOUTH);

        // Adicionar tudo à Janela Principal
        add(topPanel, BorderLayout.NORTH);
        add(centerWrapperPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Ações (Listeners)
        loadButton.addActionListener(e -> load());
        classifyButton.addActionListener(e -> classifyy());
        changeFileButton.addActionListener(e -> changeFile());
        clearDataButton.addActionListener(e -> clearData());
        
        log("Bem-vindo à Aplicação de Classificação.");
        log("Por favor, clique em 'Carregar Rede' para escolher o ficheiro bn. pretendido.");
    }
    
    // ====================== MÉTODOS DE ATUALIZAÇÃO DE BORDAS ======================
    
    private void updateStep1Border(Color color) {
        topPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(color, 3),
                "Carregar Modelo",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, 
                titleFont,
                color));
    }
    
    private void updateStep2Border(Color color) {
        centerWrapperPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(color, 3),
                "Atributos",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, 
                titleFont,
                color.equals(COLOR_WHITE) ? Color.BLACK : color));
    }

    // ====================== AÇÕES DOS BOTÕES ======================

    private void load() {
        JFileChooser fc = new JFileChooser(".");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fc.getSelectedFile()))) {
                
                loadedNetwork = (BN) ois.readObject();
                
                log("\n>>> Rede carregada: " + fc.getSelectedFile().getName());
                
                // Sucesso: Passo 1 fica verde, Passo 2 fica amarelo
                updateStep1Border(COLOR_GREEN);
                updateStep2Border(COLOR_YELLOW);
                
                // Atualizar label com nome do ficheiro
                loadedFileLabel.setText("Ficheiro: " + fc.getSelectedFile().getName());
                loadedFileLabel.setForeground(COLOR_GREEN);
                
                // Gera os campos assim que a rede é carregada
                generateInputFields();
                
                // Ativar botões
                classifyButton.setEnabled(true);
                changeFileButton.setEnabled(true);
                clearDataButton.setEnabled(true);
                
                log(">>> Por favor, preencha os atributos e depois clique em 'Classificar'.");
                
            } catch (Exception ex) {
                // Erro: Passo 1 fica vermelho
                updateStep1Border(COLOR_RED);
                loadedFileLabel.setText("Erro ao carregar ficheiro!");
                loadedFileLabel.setForeground(COLOR_RED);
                log("ERRO ao carregar: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
    
    private void changeFile() {
        // Voltar ao estado inicial
        loadedNetwork = null;
        
        // Limpar campos
        inputsPanel.removeAll();
        manualInputs.clear();
        inputsPanel.revalidate();
        inputsPanel.repaint();
        
        // Desativar botões
        classifyButton.setEnabled(false);
        changeFileButton.setEnabled(false);
        clearDataButton.setEnabled(false);
        
        // Restaurar cores: Passo 1 amarelo, Passo 2 branco
        updateStep1Border(COLOR_YELLOW);
        updateStep2Border(COLOR_WHITE);
        
        // Restaurar label
        loadedFileLabel.setText("Nenhum ficheiro carregado");
        loadedFileLabel.setForeground(Color.GRAY);
        
        log("\n>>> Ficheiro desmarcado. Por favor, carregue um novo ficheiro .bn");
    }
    
    private void clearData() {
        // Limpar todos os campos de input
        for (JTextField tf : manualInputs) {
            tf.setText("0");
        }
        
        // Passo 2 volta a amarelo
        updateStep2Border(COLOR_YELLOW);
        
        log(">>> Atributos limpos. Por favor, preencha novamente.");
    }

    private void generateInputFields() {
        inputsPanel.removeAll();
        manualInputs.clear();
        
        int totalVars = loadedNetwork.getDim(); 
        int numAttributes = totalVars - 1; // Excluindo a classe
        
        // Calcular número de colunas dinamicamente
        int pairs;
        if (numAttributes <= 5) {
            pairs = 2;  // 4 colunas (2 pares)
        } else if (numAttributes <= 10) {
            pairs = 3;  // 6 colunas (3 pares)
        } else if (numAttributes <= 20) {
            pairs = 4;  // 8 colunas (4 pares)
        } else {
            pairs = 5;  // 10 colunas (5 pares) para muitas variáveis
        }
        
        inputsPanel.setLayout(new GridLayout(0, pairs * 2, 10, 10));
        
        for (int i = 0; i < numAttributes; i++) {
            int maxVal = loadedNetwork.getDomain(i) - 1; 
            
            JLabel lbl = new JLabel("Var " + i + " [0-" + maxVal + "]:");
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            lbl.setFont(defaultFont);
            
            JTextField tf = new JTextField("0", 3);
            tf.setHorizontalAlignment(SwingConstants.CENTER);
            tf.setFont(defaultFont);
            
            manualInputs.add(tf);
            inputsPanel.add(lbl);
            inputsPanel.add(tf);
        }
        
        inputsPanel.revalidate();
        inputsPanel.repaint();
        
        if (inputsPanel.getParent() != null) {
            inputsPanel.getParent().revalidate();
            inputsPanel.getParent().repaint();
        }
    }

    private void classifyy() {
        try {
            int totalVars = loadedNetwork.getDim();
            int[] data = new int[totalVars];
            boolean hasError = false;
            
            // Primeiro, resetar todas as cores para preto
            for (JTextField tf : manualInputs) {
                tf.setForeground(Color.BLACK);
            }
            
            // Ler valores das caixas de texto
            for (int i = 0; i < manualInputs.size(); i++) {
                JTextField tf = manualInputs.get(i);
                String text = tf.getText().trim();
                
                // Validação básica
                if(text.isEmpty()) {
                    tf.setForeground(COLOR_RED);
                    hasError = true;
                    continue;
                }
                
                try {
                    int val = Integer.parseInt(text);
                    int max = loadedNetwork.getDomain(i) - 1;
                    
                    if (val < 0 || val > max) {
                        tf.setForeground(COLOR_RED);
                        hasError = true;
                    } else {
                        data[i] = val;
                    }
                } catch (NumberFormatException ex) {
                    tf.setForeground(COLOR_RED);
                    hasError = true;
                }
            }
            
            if (hasError) {
                updateStep2Border(COLOR_RED);
                JOptionPane.showMessageDialog(this, "Corrija os campos assinalados a vermelho.");
                return;
            }
            
            data[totalVars - 1] = 0; // Placeholder para a classe
            
            // Classificar
            int result = loadedNetwork.classify(data);
            
            // Calcular probabilidade
            data[totalVars - 1] = result;
            double prob = loadedNetwork.prob(data);
            
            // Formatação da probabilidade
            String probabilityText;
            if (prob < 0.0001) {
                probabilityText = String.format("%.4e", prob);
            } else {
                probabilityText = String.format("%.4f%%", prob * 100);
            }
            
            // Sucesso: Passo 2 fica verde
            updateStep2Border(COLOR_GREEN);
            
            // Apresentar resultado
            log("--------------------------------------------------");
            log("Entrada: " + Arrays.toString(Arrays.copyOf(data, totalVars - 1)));
            log("PREVISÃO: CLASSE " + result);
            log("Probabilidade Conjunta (P): " + probabilityText);
            log("--------------------------------------------------");
            
        } catch (Exception ex) {
            log("Erro na classificação: " + ex.getMessage());
        }
    }
    
    private void log(String s) { 
        logArea.append(s + "\n"); 
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        try { 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new App2_Classificacao().setVisible(true));
    }
}