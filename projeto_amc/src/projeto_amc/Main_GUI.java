package projeto_amc;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.*; // Necessário para estilizar o texto (cores)
import java.awt.*;
import java.util.Arrays;

public class Main_GUI extends JFrame {

    // Resolve o aviso "serializable class ... does not declare a static final serialVersionUID"
    private static final long serialVersionUID = 1L;

    // --- Componentes da Interface ---
    private JComboBox<String> comboFicheiros;
    private JTextPane areaLog; // Mudado de JTextArea para JTextPane para suportar cores
    private JProgressBar barraProgresso;
    private JButton btnCarregar, btnValidar, btnDiagnosticar;
    private JTextField campoIDPaciente;
    private JLabel lblStatusDados;

    // --- Lógica do Projeto ---
    private Amostra amostraCompleta;
    private Grafosorientados grafoFinal;
    private Redebayesiana classificadorFinal;
    private boolean modeloTreinado = false;

    // Lista de ficheiros disponíveis
    private final String[] ficheiros = {
            "bcancer.csv", "diabetes.csv", "hepatitis.csv",
            "letter.csv", "satimage.csv", "soybean-large.csv", "thyroid.csv"
    };

    public Main_GUI() {
        // Configuração da Janela Principal
        setTitle("Sistema de Diagnóstico com Rede Bayes - Projeto AMC");
        setSize(800, 650); // Aumentei um pouco para caber melhor o log
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centralizar no ecrã
        setLayout(new BorderLayout(10, 10));

        // 1. PAINEL SUPERIOR (Carregamento)
        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelTopo.setBorder(new TitledBorder("1. Carregar Dados"));
        
        comboFicheiros = new JComboBox<>(ficheiros);
        btnCarregar = new JButton("Carregar Dataset");
        btnCarregar.setFocusPainted(false); // Estética: remove a linha pontilhada ao clicar
        
        lblStatusDados = new JLabel("Nenhum dado carregado.");
        lblStatusDados.setForeground(Color.GRAY);
        lblStatusDados.setFont(new Font("Segoe UI", Font.BOLD, 12));

        painelTopo.add(new JLabel("Ficheiro:"));
        painelTopo.add(comboFicheiros);
        painelTopo.add(btnCarregar);
        painelTopo.add(Box.createHorizontalStrut(20)); // Espaço em branco
        painelTopo.add(lblStatusDados);

        // 2. PAINEL CENTRAL (Log "Bonito")
        areaLog = new JTextPane();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Fonte mais limpa
        areaLog.setBackground(new Color(250, 250, 250)); // Fundo cinza muito claro (suave)
        
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(new TitledBorder("Log de Execução"));
        scrollLog.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 3. PAINEL INFERIOR (Ações)
        JPanel painelFundo = new JPanel(new GridLayout(2, 1));

        // 3.1 Painel de Validação
        JPanel painelValidacao = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelValidacao.setBorder(new TitledBorder("2. Validação (Leave-One-Out)"));
        
        btnValidar = new JButton("Iniciar Validação");
        btnValidar.setEnabled(false);
        
        barraProgresso = new JProgressBar();
        barraProgresso.setStringPainted(true);
        barraProgresso.setPreferredSize(new Dimension(400, 25));
        
        painelValidacao.add(btnValidar);
        painelValidacao.add(barraProgresso);

        // 3.2 Painel de Diagnóstico Individual
        JPanel painelDiag = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelDiag.setBorder(new TitledBorder("3. Diagnóstico de Paciente Específico"));
        
        campoIDPaciente = new JTextField(6);
        btnDiagnosticar = new JButton("Testar Paciente");
        btnDiagnosticar.setEnabled(false);

        painelDiag.add(new JLabel("ID do Paciente:"));
        painelDiag.add(campoIDPaciente);
        painelDiag.add(btnDiagnosticar);

        painelFundo.add(painelValidacao);
        painelFundo.add(painelDiag);

        // Adicionar painéis à janela
        add(painelTopo, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        add(painelFundo, BorderLayout.SOUTH);

        // Configurar Ações
        configurarBotoes();
        
        // Mensagem de boas-vindas no log
        log("Bem-vindo ao Sistema de Diagnóstico.", Color.BLACK, true);
        log("Por favor, selecione um ficheiro acima para começar.\n", Color.GRAY, false);
    }

    private void configurarBotoes() {
        
        // --- AÇÃO: CARREGAR ---
        btnCarregar.addActionListener(e -> {
            String ficheiro = (String) comboFicheiros.getSelectedItem();
            areaLog.setText(""); // Limpa o log antigo
            log(">>> A carregar ficheiro: " + ficheiro + "...", Color.BLUE, true);
            
            try {
                amostraCompleta = new Amostra(ficheiro);
                if (amostraCompleta.length() == 0) {
                    log("Erro: O ficheiro está vazio!", Color.RED, true);
                    return;
                }
                
                lblStatusDados.setText("Dados: " + amostraCompleta.length() + " casos | " + amostraCompleta.dim() + " variáveis");
                lblStatusDados.setForeground(new Color(0, 100, 0)); // Verde escuro na label
                
                log("Sucesso! Dados carregados corretamente.", new Color(0, 128, 0), true);
                log("Dimensões: " + amostraCompleta.length() + " linhas x " + amostraCompleta.dim() + " colunas\n");
                
                // Resetar interface
                btnValidar.setEnabled(true);
                btnDiagnosticar.setEnabled(true);
                modeloTreinado = false;
                barraProgresso.setValue(0);
                
            } catch (Exception ex) {
                log("ERRO FATAL ao ler ficheiro: " + ex.getMessage(), Color.RED, true);
                ex.printStackTrace();
            }
        });

        // --- AÇÃO: VALIDAR ---
        btnValidar.addActionListener(e -> {
            if (amostraCompleta == null) return;
            // Executa numa Thread separada para não travar a janela
            new Thread(this::executarLeaveOneOut).start();
        });

        // --- AÇÃO: DIAGNOSTICAR ---
        btnDiagnosticar.addActionListener(e -> diagnosticarPaciente());
    }

    // --- LÓGICA DO LEAVE-ONE-OUT (Background) ---
    private void executarLeaveOneOut() {
        SwingUtilities.invokeLater(() -> {
            btnValidar.setEnabled(false);
            btnCarregar.setEnabled(false);
        });
        
        log("=== INICIANDO VALIDAÇÃO (LEAVE-ONE-OUT) ===", Color.BLUE, true);
        
        long inicio = System.currentTimeMillis();
        int acertos = 0;
        int total = amostraCompleta.length();
        
        barraProgresso.setMinimum(0);
        barraProgresso.setMaximum(total);
        barraProgresso.setValue(0);

        for (int i = 0; i < total; i++) {
            try {
                Amostra treino = amostraCompleta.amostraSem(i);
                int[] teste = amostraCompleta.element(i);
                int classeReal = teste[teste.length - 1];

                Grafosorientados grafo = new Grafosorientados(treino.dim());
                grafo.aprender(treino);

                Redebayesiana classificador = new Redebayesiana(grafo, treino, 0.5);

                int[] testeSemClasse = teste.clone();
                testeSemClasse[testeSemClasse.length - 1] = 0; // Oculta a classe

                int previsao = classificador.classificar(testeSemClasse);

                if (previsao == classeReal) acertos++;

                // Atualizar barra
                int progresso = i + 1;
                SwingUtilities.invokeLater(() -> barraProgresso.setValue(progresso));
                
            } catch (Exception ex) {
                log("Erro na iteração " + i + ": " + ex.getMessage(), Color.RED, false);
            }
        }

        long fim = System.currentTimeMillis();
        double accuracy = (double) acertos / total * 100;
        
        // Log final dos resultados
        log("\n=== RESULTADOS DA VALIDAÇÃO ===", Color.BLUE, true);
        log("Total de Casos: " + total);
        log("Acertos: " + acertos);
        
        // Decide a cor da precisão (Verde se for bom, Laranja se for médio, Vermelho se for mau)
        Color corResult = accuracy > 80 ? new Color(0, 128, 0) : (accuracy > 50 ? Color.ORANGE : Color.RED);
        log(String.format("PRECISÃO FINAL: %.2f%%", accuracy), corResult, true);
        
        log("Tempo de execução: " + (fim - inicio) / 1000.0 + " segundos\n");
        
        SwingUtilities.invokeLater(() -> {
            btnValidar.setEnabled(true);
            btnCarregar.setEnabled(true);
        });
    }

    // --- LÓGICA DO DIAGNÓSTICO INDIVIDUAL ---
    private void diagnosticarPaciente() {
        try {
            String textoInput = campoIDPaciente.getText().trim();
            if (textoInput.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Digite um ID primeiro.");
                return;
            }
            
            int id = Integer.parseInt(textoInput);
            if (id < 0 || id >= amostraCompleta.length()) {
                log("Erro: ID " + id + " não existe. Use entre 0 e " + (amostraCompleta.length()-1), Color.RED, true);
                return;
            }

            // Treina o modelo completo apenas se necessário (cache)
            if (!modeloTreinado) {
                log(">> A treinar modelo final com TODOS os dados...", Color.DARK_GRAY, false);
                grafoFinal = new Grafosorientados(amostraCompleta.dim());
                grafoFinal.aprender(amostraCompleta);
                classificadorFinal = new Redebayesiana(grafoFinal, amostraCompleta, 0.5);
                modeloTreinado = true;
                log(">> Modelo treinado e pronto.", new Color(0, 128, 0), false);
            }

            // Processo de classificação
            int[] dadosReais = amostraCompleta.element(id);
            int classeReal = dadosReais[dadosReais.length - 1];
            
            int[] dadosParaTeste = dadosReais.clone();
            dadosParaTeste[dadosParaTeste.length - 1] = 0; 

            int previsao = classificadorFinal.classificar(dadosParaTeste);

            // Mostrar resultado formatado
            log("\n--- Paciente #" + id + " ---", Color.BLUE, true);
            log("Dados: " + Arrays.toString(dadosReais));
            
            String status = (classeReal == previsao) ? "CORRETO [OK]" : "INCORRETO [X]";
            Color corStatus = (classeReal == previsao) ? new Color(0, 128, 0) : Color.RED;
            
            log("Real: " + classeReal + " | Previsto: " + previsao);
            log("RESULTADO: " + status, corStatus, true);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "ID inválido! Insira apenas números.");
        } catch (Exception ex) {
            log("Erro ao diagnosticar: " + ex.getMessage(), Color.RED, true);
        }
    }

    // --- MÉTODO AUXILIAR PARA LOG ESTILIZADO (CORES) ---
    private void log(String texto, Color cor, boolean negrito) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = areaLog.getStyledDocument();
            
            SimpleAttributeSet estilo = new SimpleAttributeSet();
            StyleConstants.setForeground(estilo, cor);
            StyleConstants.setBold(estilo, negrito);
            StyleConstants.setFontSize(estilo, 14);
            StyleConstants.setFontFamily(estilo, "Segoe UI");

            try {
                doc.insertString(doc.getLength(), texto + "\n", estilo);
                areaLog.setCaretPosition(doc.getLength()); // Auto-scroll para o fim
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    // Sobrecarga para log simples (preto)
    private void log(String texto) {
        log(texto, Color.BLACK, false);
    }

    public static void main(String[] args) {
        // Tenta usar o visual nativo do Windows/Mac/Linux
        try { 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new Main_GUI().setVisible(true);
        });
    }
}