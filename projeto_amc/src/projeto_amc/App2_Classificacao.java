package projeto_amc;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays; 

/**
 * Aplicação 2: Classificação e Diagnóstico
 * Requisito: Interface sequencial, texto preto, controlo pelo utilizador.
 */
public class App2_Classificacao extends JFrame {
    
    private static final long serialVersionUID = 1L;

    private JButton btnCarregar, btnClassificar;
    private JTextArea areaLog;
    
    // A nossa classe BN
    private BN redeCarregada;
    
    private ArrayList<JTextField> inputsManuais = new ArrayList<>();
    private JPanel painelInputs;

    public App2_Classificacao() {
        setTitle("Aplicação 2: Classificação (BN)");
        setSize(750, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrar no ecrã
        setLayout(new BorderLayout(10, 10));

        // =================================================================
        // PAINEL SUPERIOR: Passo 1 (Carregar)
        // =================================================================
        JPanel pTopo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        pTopo.setBorder(BorderFactory.createTitledBorder("Passo 1: Carregar Modelo"));

        btnCarregar = new JButton("Carregar Rede (.bn)");
        // Requisito: Texto a Preto
        btnCarregar.setForeground(Color.BLACK);
        btnCarregar.setBackground(new Color(220, 220, 220)); // Cinzento claro
        btnCarregar.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        pTopo.add(btnCarregar);

        // =================================================================
        // PAINEL CENTRAL: Passo 2 (Inputs)
        // =================================================================
        // Usamos um painel 'wrapper' para colocar os inputs no topo
        JPanel pCentroWrapper = new JPanel(new BorderLayout());
        pCentroWrapper.setBorder(BorderFactory.createTitledBorder("Passo 2: Atributos do Paciente"));
        
        painelInputs = new JPanel(new GridLayout(0, 4, 10, 10)); // Grid dinâmico
        
        // Botão Classificar (Fica junto aos inputs pois depende deles)
        JPanel pBotoesAcao = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnClassificar = new JButton("Classificar Paciente");
        // Requisito: Texto a Preto
        btnClassificar.setForeground(Color.BLACK);
        btnClassificar.setBackground(new Color(144, 238, 144)); // Verde Claro
        btnClassificar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnClassificar.setEnabled(false); // Só ativa depois de carregar a rede
        
        pBotoesAcao.add(btnClassificar);

        pCentroWrapper.add(new JScrollPane(painelInputs), BorderLayout.CENTER);
        pCentroWrapper.add(pBotoesAcao, BorderLayout.SOUTH);

        // =================================================================
        // PAINEL INFERIOR: Passo 3 (Resultados/Log)
        // =================================================================
        areaLog = new JTextArea(12, 40); 
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Passo 3: Resultados e Log"));

        // Adicionar tudo à Janela Principal
        add(pTopo, BorderLayout.NORTH);
        add(pCentroWrapper, BorderLayout.CENTER);
        add(scrollLog, BorderLayout.SOUTH);

        // Ações (Listeners)
        btnCarregar.addActionListener(e -> carregar());
        btnClassificar.addActionListener(e -> classificar());
        
        log("Bem-vindo à Aplicação de Classificação.");
        log("Por favor, clique em 'Carregar Rede' para escolher um ficheiro .bn");
    }

    private void carregar() {
        // O utilizador decide quando abre o ficheiro
        JFileChooser fc = new JFileChooser(".");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fc.getSelectedFile()))) {
                
                redeCarregada = (BN) ois.readObject();
                
                log("\n>>> Rede carregada: " + fc.getSelectedFile().getName());
                
                // Gera os campos assim que a rede é carregada
                gerarCamposInput();
                
                btnClassificar.setEnabled(true);
                log(">>> Pronto. Preencha os valores acima e clique em 'Classificar'.");
                
            } catch (Exception ex) {
                log("ERRO ao carregar: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void gerarCamposInput() {
        painelInputs.removeAll();
        inputsManuais.clear();
        
        int nVars = redeCarregada.getDim(); 
        
        // O último atributo é a classe, ignoramos no input
        for (int i = 0; i < nVars - 1; i++) {
            int maxVal = redeCarregada.getDomain(i) - 1; 
            
            JLabel lbl = new JLabel("Var " + i + " [0-" + maxVal + "]:");
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            
            JTextField tf = new JTextField("0");
            tf.setHorizontalAlignment(SwingConstants.CENTER);
            
            inputsManuais.add(tf);
            painelInputs.add(lbl);
            painelInputs.add(tf);
        }
        
        painelInputs.revalidate();
        painelInputs.repaint();
    }

    private void classificar() {
        try {
            int nTotal = redeCarregada.getDim();
            int[] dados = new int[nTotal];
            
            // Ler valores das caixas de texto
            for (int i = 0; i < inputsManuais.size(); i++) {
                String texto = inputsManuais.get(i).getText().trim();
                
                // Validação básica
                if(texto.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Preencha todos os campos.");
                    return;
                }
                
                int val = Integer.parseInt(texto);
                int max = redeCarregada.getDomain(i) - 1;
                
                if (val < 0 || val > max) {
                    JOptionPane.showMessageDialog(this, "Erro na Var " + i + ": Valor deve estar entre 0 e " + max);
                    return;
                }
                dados[i] = val;
            }
            
            dados[nTotal - 1] = 0; // Placeholder para a classe
            
            // Classificar
            int resultado = redeCarregada.classificar(dados);
            
         // Calcular probabilidade
            dados[nTotal - 1] = resultado;
            double prob = redeCarregada.prob(dados);
            
            // --- CORREÇÃO DE VISUALIZAÇÃO ---
            // 1. Criar uma string com o formato correto
            String textoProbabilidade;
            
            if (prob < 0.0001) {
                // Se for muito pequeno (ex: 1.45e-18), usa Notação Científica
                textoProbabilidade = String.format("%.4e", prob);
            } else {
                // Se for razoável (ex: 0.19), usa Percentagem
                textoProbabilidade = String.format("%.4f%%", prob * 100);
            }
            
            // 2. Apresentar resultado final (Usando a string formatada)
            log("--------------------------------------------------");
            log("Entrada: " + Arrays.toString(Arrays.copyOf(dados, nTotal - 1)));
            log("PREVISÃO: CLASSE " + resultado);
            log("Probabilidade Conjunta (P): " + textoProbabilidade);
            log("--------------------------------------------------");
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Por favor insira apenas números inteiros.");
        } catch (Exception ex) {
            log("Erro na classificação: " + ex.getMessage());
        }
    }
    
    private void log(String s) { 
        areaLog.append(s + "\n"); 
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    public static void main(String[] args) {
        try { 
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new App2_Classificacao().setVisible(true));
    }
}