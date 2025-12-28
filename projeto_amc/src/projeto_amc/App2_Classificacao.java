package projeto_amc;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 * Aplicação 2: Carregamento da Rede Bayesiana e classificação manual
 * Requisito do enunciado (pág. 29): lê a rede do disco, permite inserir parâmetros do paciente e classifica-o
 * Usa getters da Redebayesiana para obter dim() e domain()
 */
public class App2_Classificacao extends JFrame {

    private JButton btnCarregar, btnClassificar;
    private JPanel painelInputs;
    private JTextArea areaLog;
    private Redebayesiana rede;
    private ArrayList<JTextField> campos = new ArrayList<>();

    public App2_Classificacao() {
        setTitle("Aplicação 2 - Classificação de Pacientes");
        setSize(750, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Botões superiores
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        painelBotoes.setBorder(BorderFactory.createTitledBorder("Controles"));

        btnCarregar = new JButton("Carregar Rede (.bn)");
        btnClassificar = new JButton("Classificar Paciente");
        btnClassificar.setEnabled(false);

        painelBotoes.add(btnCarregar);
        painelBotoes.add(btnClassificar);

        // Painel de inputs
        painelInputs = new JPanel();
        painelInputs.setLayout(new GridLayout(0, 2, 10, 10));
        JScrollPane scrollInputs = new JScrollPane(painelInputs);
        scrollInputs.setBorder(BorderFactory.createTitledBorder("Parâmetros do Paciente"));

        // Log inferior
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Resultado"));
        scrollLog.setPreferredSize(new Dimension(0, 150));

        add(painelBotoes, BorderLayout.NORTH);
        add(scrollInputs, BorderLayout.CENTER);
        add(scrollLog, BorderLayout.SOUTH);

        btnCarregar.addActionListener(e -> carregarRede());
        btnClassificar.addActionListener(e -> classificar());

        log("Aplicação iniciada. Carregue uma rede .bn para começar.");
    }

    private void carregarRede() {
        JFileChooser fc = new JFileChooser(".");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Rede Bayesiana", "bn"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fc.getSelectedFile()))) {
                rede = (Redebayesiana) ois.readObject();
                log(">>> Rede carregada: " + fc.getSelectedFile().getName());
                log(">>> Atributos: " + (rede.getDim() - 1) + " | Classes: " + rede.getDomain(rede.getDim() - 1));
                gerarInputs();
                btnClassificar.setEnabled(true);
            } catch (Exception ex) {
                log("ERRO ao carregar: " + ex.getMessage());
            }
        }
    }

    private void gerarInputs() {
        painelInputs.removeAll();
        campos.clear();
        int numAttrs = rede.getDim() - 1;

        for (int i = 0; i < numAttrs; i++) {
            int max = rede.getDomain(i) - 1;
            JLabel lbl = new JLabel("Atributo " + i + " [0-" + max + "]:");
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            JTextField tf = new JTextField("0", 10);
            tf.setToolTipText("Valor entre 0 e " + max);

            campos.add(tf);
            painelInputs.add(lbl);
            painelInputs.add(tf);
        }

        painelInputs.revalidate();
        painelInputs.repaint();
        log(">>> Campos gerados. Preencha e classifique.");
    }

    private void classificar() {
        if (rede == null) return;

        try {
            int[] paciente = new int[rede.getDim()];
            int numAttrs = rede.getDim() - 1;

            for (int i = 0; i < numAttrs; i++) {
                int val = Integer.parseInt(campos.get(i).getText().trim());
                int max = rede.getDomain(i) - 1;
                if (val < 0 || val > max) throw new IllegalArgumentException("Atributo " + i + " inválido (0-" + max + ")");
                paciente[i] = val;
            }

            int classe = rede.classificar(paciente);

            log("\n=== RESULTADO ===");
            log("Classe prevista: " + classe);
            log("==================\n");

        } catch (Exception ex) {
            log("ERRO: " + ex.getMessage());
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
        SwingUtilities.invokeLater(() -> new App2_Classificacao().setVisible(true));
    }
}