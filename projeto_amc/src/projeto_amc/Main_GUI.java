package projeto_amc;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List; // Import necessário para a lista de inputs

public class Main_GUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // --- Componentes da Interface ---
    private JComboBox<String> comboFicheiros;
    private JTextPane areaLog;
    
    // Barras de Progresso
    private JProgressBar barraProgresso;      // Para Leave-One-Out
    private JProgressBar barraProgressoKFold; // Para K-Fold
    
    // Botões e Campos
    private JButton btnCarregar, btnValidar, btnDiagnosticar;
    private JButton btnKFold; 
    private JButton btnManual; // <--- NOVO BOTÃO
    private JTextField campoIDPaciente;
    private JTextField campoK; 
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
        setTitle("Sistema de Diagnóstico Bayesiano (Otimizado)");
        setSize(900, 850); // Aumentei um pouco a altura para caber o novo painel
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 1. PAINEL SUPERIOR (Carregamento)
        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelTopo.setBorder(new TitledBorder("1. Carregar Dados"));
        painelTopo.setBackground(new Color(240, 248, 255)); // Azul muito claro
        
        comboFicheiros = new JComboBox<>(ficheiros);
        btnCarregar = new JButton("Carregar Dataset");
        btnCarregar.setBackground(new Color(70, 130, 180));
        btnCarregar.setForeground(Color.WHITE);
        btnCarregar.setFocusPainted(false);
        
        lblStatusDados = new JLabel("Nenhum dado carregado.");
        lblStatusDados.setForeground(Color.GRAY);
        lblStatusDados.setFont(new Font("Segoe UI", Font.BOLD, 12));

        painelTopo.add(new JLabel("Ficheiro:"));
        painelTopo.add(comboFicheiros);
        painelTopo.add(btnCarregar);
        painelTopo.add(Box.createHorizontalStrut(20));
        painelTopo.add(lblStatusDados);

        // 2. PAINEL CENTRAL (Log)
        areaLog = new JTextPane();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Consolas", Font.PLAIN, 13)); // Fonte monoespaçada para alinhar tabelas
        areaLog.setBackground(new Color(250, 250, 250));
        
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(new TitledBorder("Log de Execução"));
        scrollLog.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 3. PAINEL INFERIOR (Ações)
        // Alterado para 4 linhas para caber a nova secção
        JPanel painelFundo = new JPanel(new GridLayout(4, 1)); 

        // 3.1 Painel de Validação (Leave-One-Out)
        JPanel painelValidacao = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelValidacao.setBorder(new TitledBorder("2.A Validação (Leave-One-Out)"));
        
        btnValidar = new JButton("Iniciar Leave-One-Out");
        btnValidar.setEnabled(false);
        
        barraProgresso = new JProgressBar();
        barraProgresso.setStringPainted(true);
        barraProgresso.setPreferredSize(new Dimension(300, 25));
        
        painelValidacao.add(btnValidar);
        painelValidacao.add(barraProgresso);

        // 3.2 Painel K-Fold Cross Validation
        JPanel painelKFoldWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelKFoldWrapper.setBorder(new TitledBorder("2.B Validação (K-Fold Cross Validation)"));

        JLabel lblK = new JLabel("Valor de K:");
        campoK = new JTextField("10", 3);
        btnKFold = new JButton("Iniciar K-Fold");
        btnKFold.setEnabled(false);
        
        barraProgressoKFold = new JProgressBar();
        barraProgressoKFold.setStringPainted(true);
        barraProgressoKFold.setPreferredSize(new Dimension(300, 25));

        painelKFoldWrapper.add(lblK);
        painelKFoldWrapper.add(campoK);
        painelKFoldWrapper.add(btnKFold);
        painelKFoldWrapper.add(Box.createHorizontalStrut(10));
        painelKFoldWrapper.add(barraProgressoKFold);

        // 3.A Painel de Diagnóstico Individual (ID)
        JPanel painelDiag = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelDiag.setBorder(new TitledBorder("3.A Diagnóstico por ID (Existente no Ficheiro)"));
        
        campoIDPaciente = new JTextField(6);
        btnDiagnosticar = new JButton("Testar ID");
        btnDiagnosticar.setEnabled(false);

        painelDiag.add(new JLabel("ID do Paciente (Índice):"));
        painelDiag.add(campoIDPaciente);
        painelDiag.add(btnDiagnosticar);

        // 3.B Painel de Diagnóstico Manual (NOVA SECÇÃO)
        JPanel painelManualWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelManualWrapper.setBorder(new TitledBorder("3.B Diagnóstico Manual (Inserir Variáveis)"));
        
        btnManual = new JButton("Inserir Variáveis Manualmente");
        btnManual.setEnabled(false); // Só ativa quando carrega dados
        
        painelManualWrapper.add(new JLabel("Simular novo paciente: "));
        painelManualWrapper.add(btnManual);

        // Adicionar painéis ao fundo
        painelFundo.add(painelValidacao);
        painelFundo.add(painelKFoldWrapper);
        painelFundo.add(painelDiag);
        painelFundo.add(painelManualWrapper); // Adicionado aqui

        add(painelTopo, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        add(painelFundo, BorderLayout.SOUTH);

        configurarBotoes();
        
        log("Sistema iniciado. Carregue um ficheiro para começar.", Color.BLACK, true);
    }

    private void configurarBotoes() {
        // --- AÇÃO: CARREGAR ---
        btnCarregar.addActionListener(e -> {
            new Thread(() -> { // Carregar em thread separada para não bloquear GUI
                String ficheiro = (String) comboFicheiros.getSelectedItem();
                
                SwingUtilities.invokeLater(() -> {
                    areaLog.setText(""); 
                    btnCarregar.setEnabled(false);
                    log(">>> A carregar ficheiro: " + ficheiro + "...", Color.BLUE, true);
                });

                try {
                    Amostra tempAmostra = new Amostra(ficheiro);
                    
                    SwingUtilities.invokeLater(() -> {
                        if (tempAmostra.length() == 0) {
                            log("Erro: O ficheiro está vazio!", Color.RED, true);
                            btnCarregar.setEnabled(true);
                            return;
                        }
                        amostraCompleta = tempAmostra;
                        
                        lblStatusDados.setText("Dados: " + amostraCompleta.length() + " linhas | " + amostraCompleta.dim() + " colunas");
                        lblStatusDados.setForeground(new Color(0, 100, 0));
                        
                        log("Sucesso! Dados carregados.", new Color(0, 128, 0), true);
                        log("Dimensões: " + amostraCompleta.length() + " x " + amostraCompleta.dim());
                        
                        long complexidade = (long) amostraCompleta.length() * amostraCompleta.dim();
                        if (complexidade > 50000) {
                            log("Recomendado usar K-Fold em vez de Leave-One-Out.", Color.GRAY, false);
                        }

                        // Habilitar botões
                        btnValidar.setEnabled(true);
                        btnKFold.setEnabled(true);
                        btnDiagnosticar.setEnabled(true);
                        btnManual.setEnabled(true); // Habilita o novo botão
                        btnCarregar.setEnabled(true);
                        modeloTreinado = false;
                        
                        barraProgresso.setValue(0);
                        barraProgressoKFold.setValue(0);
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        log("ERRO ao ler ficheiro: " + ex.getMessage(), Color.RED, true);
                        ex.printStackTrace();
                        btnCarregar.setEnabled(true);
                    });
                }
            }).start();
        });

        // --- AÇÃO: VALIDAR (Leave-One-Out) ---
        btnValidar.addActionListener(e -> {
            if (amostraCompleta == null) return;
            if (amostraCompleta.length() > 5000) {
                int resp = JOptionPane.showConfirmDialog(this, 
                    "Este dataset é muito grande para Leave-One-Out.\nVai demorar muito tempo.\nDeseja continuar mesmo assim?",
                    "Aviso de Performance", JOptionPane.YES_NO_OPTION);
                if (resp != JOptionPane.YES_OPTION) return;
            }
            new Thread(this::executarLeaveOneOut).start();
        });

        // --- AÇÃO: K-FOLD ---
        btnKFold.addActionListener(e -> {
            if (amostraCompleta == null) return;
            new Thread(this::executarKFold).start();
        });

        // --- AÇÃO: DIAGNOSTICAR POR ID ---
        btnDiagnosticar.addActionListener(e -> diagnosticarPaciente());

        // --- AÇÃO: DIAGNOSTICAR MANUAL (NOVA) ---
        btnManual.addActionListener(e -> abrirInputManual());
    }

    // --- NOVA FUNÇÃO: Janela de Input Manual ---
        // --- NOVA FUNÇÃO OTIMIZADA: Janela de Input Manual com Limites ---
        private void abrirInputManual() {
            if (amostraCompleta == null) return;

            // O último atributo é a classe, por isso pedimos dim() - 1 variáveis
            int numVariaveis = amostraCompleta.dim() - 1;
            
            // Criar painel dinâmico
            JPanel panelInputs = new JPanel(new GridLayout(0, 2, 10, 10)); // Espaçamento maior
            List<JTextField> camposTexto = new ArrayList<>();

            for (int i = 0; i < numVariaveis; i++) {
                // Obter o limite máximo para esta variável (Domínio - 1)
                int limiteMax = amostraCompleta.domain(i) - 1;
                
                // Criar a etiqueta com a "cábula" dos limites
                String textoLabel = "Var " + i + " [0 - " + limiteMax + "]:";
                JLabel label = new JLabel(textoLabel);
                label.setHorizontalAlignment(SwingConstants.RIGHT); // Alinhar à direita para ficar bonito
                
                JTextField tf = new JTextField("0"); // Valor default
                
                // Adicionar tooltip para ajudar o utilizador
                tf.setToolTipText("Insira um valor inteiro entre 0 e " + limiteMax);
                
                camposTexto.add(tf);
                panelInputs.add(label);
                panelInputs.add(tf);
            }

            // Colocar num ScrollPane caso sejam muitas variáveis (ex: Letter tem 16)
            JScrollPane scrollPane = new JScrollPane(panelInputs);
            scrollPane.setPreferredSize(new Dimension(350, 400)); // Um pouco maior
            scrollPane.setBorder(null);

            int result = JOptionPane.showConfirmDialog(this, scrollPane, 
                    "Inserir valores manuais (" + numVariaveis + " variáveis)", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    // Construir o array com os inputs
                    int[] dadosManuais = new int[amostraCompleta.dim()];
                    
                    for (int i = 0; i < numVariaveis; i++) {
                        String texto = camposTexto.get(i).getText().trim();
                        
                        // 1. Verificar se é número
                        int valor = Integer.parseInt(texto);
                        
                        // 2. Verificar se está dentro dos limites do domínio
                        int limiteMax = amostraCompleta.domain(i) - 1;
                        
                        if (valor < 0 || valor > limiteMax) {
                            throw new IllegalArgumentException("A Variável " + i + " deve estar entre 0 e " + limiteMax + ".\nValor inserido: " + valor);
                        }
                        
                        dadosManuais[i] = valor;
                    }
                    
                    dadosManuais[amostraCompleta.dim() - 1] = 0; // A classe é desconhecida (0 placeholder)

                    // Enviar para classificação (Lógica partilhada)
                    processarClassificacao(dadosManuais, -1); // -1 indica que é manual

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Erro: Insira apenas números inteiros!", "Formato Inválido", JOptionPane.ERROR_MESSAGE);
                    // Reabrir para o utilizador não perder tudo? (Opcional, mas aqui fecha para simplificar)
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Valor Fora dos Limites", JOptionPane.WARNING_MESSAGE);
                }
            }
        }

    // --- LÓGICA DO K-FOLD CROSS VALIDATION ---
    private void executarKFold() {
        bloquearBotoes(true);
        int kInput = 10;
        try {
            kInput = Integer.parseInt(campoK.getText().trim());
            if (kInput <= 1 || kInput > amostraCompleta.length()) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            log("Erro: K inválido.", Color.RED, true);
            bloquearBotoes(false);
            return;
        }

        final int k = kInput;
        log("\n=== INICIANDO " + k + "-FOLD CROSS VALIDATION ===", Color.BLUE, true);
        
        long inicio = System.currentTimeMillis();
        int totalAcertos = 0;
        int totalCasosTestados = 0;
        int totalDados = amostraCompleta.length();

        barraProgressoKFold.setMinimum(0);
        barraProgressoKFold.setMaximum(k); 
        barraProgressoKFold.setValue(0);

        try {
            for (int fold = 0; fold < k; fold++) {
                Amostra treino = new Amostra();
                ArrayList<int[]> listaTeste = new ArrayList<>();

                for (int i = 0; i < totalDados; i++) {
                    if (i % k == fold) {
                        listaTeste.add(amostraCompleta.element(i));
                    } else {
                        treino.add(amostraCompleta.element(i));
                    }
                }

                Grafosorientados grafo = new Grafosorientados(treino.dim());
                grafo.aprender(treino); 
                Redebayesiana classificador = new Redebayesiana(grafo, treino, 0.5);

                int acertosNoFold = 0;
                for (int[] casoTeste : listaTeste) {
                    int classeReal = casoTeste[casoTeste.length - 1];
                    int[] casoSemClasse = casoTeste.clone();
                    casoSemClasse[casoSemClasse.length - 1] = 0;

                    int previsao = classificador.classificar(casoSemClasse);
                    if (previsao == classeReal) {
                        acertosNoFold++;
                        totalAcertos++;
                    }
                    totalCasosTestados++;
                }

                int progresso = fold + 1;
                final int acertosAtuais = acertosNoFold;
                final int totalTesteAtual = listaTeste.size();
                
                SwingUtilities.invokeLater(() -> {
                    barraProgressoKFold.setValue(progresso);
                    log("Fold " + progresso + "/" + k + ": " + acertosAtuais + "/" + totalTesteAtual + " acertos.");
                });
            }

            long fim = System.currentTimeMillis();
            double accuracy = (totalCasosTestados > 0) ? (double) totalAcertos / totalCasosTestados * 100 : 0;

            log("\n=== RESULTADOS FINAIS K-FOLD ===", Color.BLUE, true);
            Color corResult = accuracy > 80 ? new Color(0, 128, 0) : (accuracy > 50 ? Color.ORANGE : Color.RED);
            log(String.format("PRECISÃO GLOBAL: %.2f%%", accuracy), corResult, true);
            log("Tempo total: " + (fim - inicio) / 1000.0 + "s\n");

        } catch (Exception ex) {
            log("Erro fatal no K-Fold: " + ex.getMessage(), Color.RED, true);
            ex.printStackTrace();
        } finally {
            bloquearBotoes(false);
        }
    }

    // --- LÓGICA DO LEAVE-ONE-OUT ---
    private void executarLeaveOneOut() {
        bloquearBotoes(true);
        log("\n=== INICIANDO LEAVE-ONE-OUT ===", Color.BLUE, true);
        
        long inicio = System.currentTimeMillis();
        int acertos = 0;
        int total = amostraCompleta.length();
        
        barraProgresso.setMinimum(0);
        barraProgresso.setMaximum(total);
        barraProgresso.setValue(0);

        int logStep = Math.max(1, total / 20);

        for (int i = 0; i < total; i++) {
            try {
                Amostra treino = amostraCompleta.amostraSem(i);
                int[] teste = amostraCompleta.element(i);
                int classeReal = teste[teste.length - 1];

                Grafosorientados grafo = new Grafosorientados(treino.dim());
                grafo.aprender(treino);
                Redebayesiana classificador = new Redebayesiana(grafo, treino, 0.5);

                int[] testeSemClasse = teste.clone();
                testeSemClasse[testeSemClasse.length - 1] = 0; 

                int previsao = classificador.classificar(testeSemClasse);

                if (previsao == classeReal) acertos++;

                int progresso = i + 1;
                if (i % logStep == 0 || i == total - 1) {
                    SwingUtilities.invokeLater(() -> barraProgresso.setValue(progresso));
                }
                
            } catch (Exception ex) {
                System.err.println("Erro na iteração " + i);
            }
        }

        long fim = System.currentTimeMillis();
        double accuracy = (double) acertos / total * 100;
        
        log("\n=== RESULTADOS LEAVE-ONE-OUT ===", Color.BLUE, true);
        Color corResult = accuracy > 80 ? new Color(0, 128, 0) : (accuracy > 50 ? Color.ORANGE : Color.RED);
        log(String.format("PRECISÃO FINAL: %.2f%%", accuracy), corResult, true);
        log("Tempo total: " + (fim - inicio) / 1000.0 + "s\n");
        
        bloquearBotoes(false);
    }

    // --- DIAGNÓSTICO INDIVIDUAL (Lógica de ID) ---
    private void diagnosticarPaciente() {
        try {
            String textoInput = campoIDPaciente.getText().trim();
            if (textoInput.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Digite um ID numérico.");
                return;
            }
            
            int id = Integer.parseInt(textoInput);
            if (id < 0 || id >= amostraCompleta.length()) {
                log("Erro: ID fora dos limites (0 a " + (amostraCompleta.length()-1) + ")", Color.RED, true);
                return;
            }

            int[] dadosReais = amostraCompleta.element(id);
            processarClassificacao(dadosReais, id);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "ID inválido! Insira apenas números.");
        }
    }
    
    // --- LÓGICA CENTRAL DE CLASSIFICAÇÃO (Partilhada por ID e Manual) ---
    // Se idOriginal for -1, significa que é um teste manual
    private void processarClassificacao(int[] dadosComClasse, int idOriginal) {
        
        // Treino preguiçoso (Lazy Training) se ainda não tiver sido feito
        if (!modeloTreinado) {
            log(">> A treinar modelo mestre com TODOS os dados...", Color.DARK_GRAY, false);
            bloquearBotoes(true);

            new Thread(() -> {
                try {
                    grafoFinal = new Grafosorientados(amostraCompleta.dim());
                    grafoFinal.aprender(amostraCompleta);
                    classificadorFinal = new Redebayesiana(grafoFinal, amostraCompleta, 0.5);
                    modeloTreinado = true;
                    
                    SwingUtilities.invokeLater(() -> {
                        log(">> Modelo treinado com sucesso.", new Color(0, 128, 0), false);
                        bloquearBotoes(false);
                        // Executa a classificação real agora
                        executarClassificacaoInterna(dadosComClasse, idOriginal);
                    });
                } catch(Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        log("Erro no treino: " + ex.getMessage(), Color.RED, true);
                        bloquearBotoes(false);
                    });
                }
            }).start();
        } else {
            executarClassificacaoInterna(dadosComClasse, idOriginal);
        }
    }

    private void executarClassificacaoInterna(int[] dadosComClasse, int idOriginal) {
        // Preparar dados (esconder classe)
        int[] dadosParaTeste = dadosComClasse.clone();
        int classeReal = dadosComClasse[dadosComClasse.length - 1];
        dadosParaTeste[dadosParaTeste.length - 1] = 0; // Ocultar

        // Classificar
        int previsao = classificadorFinal.classificar(dadosParaTeste);

        // Mostrar Resultados
        if (idOriginal != -1) {
            log("\n--- Paciente ID #" + idOriginal + " (Existente) ---", Color.BLUE, true);
            log("Atributos: " + Arrays.toString(dadosParaTeste));
            
            String status = (classeReal == previsao) ? "ACERTOU" : "ERROU";
            Color corStatus = (classeReal == previsao) ? new Color(0, 128, 0) : Color.RED;
            
            log("Real: " + classeReal + " | Previsto: " + previsao);
            log("RESULTADO: " + status, corStatus, true);
        } else {
            log("\n--- Paciente Simulado (Manual) ---", new Color(139, 0, 139), true); // Roxo
            log("Atributos Inseridos: " + Arrays.toString(dadosParaTeste));
            log("PREVISÃO DO SISTEMA: CLASSE " + previsao, new Color(0, 0, 139), true);
            log("(Nota: Como é manual, não sabemos a classe real para comparar)");
        }
    }

    private void bloquearBotoes(boolean bloquear) {
        SwingUtilities.invokeLater(() -> {
            boolean estado = !bloquear;
            btnValidar.setEnabled(estado);
            btnKFold.setEnabled(estado);
            btnCarregar.setEnabled(estado);
            btnDiagnosticar.setEnabled(estado);
            btnManual.setEnabled(estado);
        });
    }

    // --- UTILITÁRIOS ---
    private void log(String texto, Color cor, boolean negrito) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = areaLog.getStyledDocument();
            SimpleAttributeSet estilo = new SimpleAttributeSet();
            StyleConstants.setForeground(estilo, cor);
            StyleConstants.setBold(estilo, negrito);
            StyleConstants.setFontSize(estilo, 13);
            StyleConstants.setFontFamily(estilo, "Consolas");

            try {
                doc.insertString(doc.getLength(), texto + "\n", estilo);
                areaLog.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    private void log(String texto) {
        log(texto, Color.BLACK, false);
    }

    public static void main(String[] args) {
        try { 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new Main_GUI().setVisible(true);
        });
    }
}