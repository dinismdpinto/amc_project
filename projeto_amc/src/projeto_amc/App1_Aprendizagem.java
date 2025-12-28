package projeto_amc;

import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

/**
 * Aplicação 1: Aprendizagem da Rede Bayesiana e gravação em disco
 * Requisito do enunciado (pág. 29): lê a amostra, aprende a rede e grava-a no disco
 * Parâmetros configuráveis: k (máx pais atributos) e número de restarts aleatórios
 */
public class App1_Aprendizagem extends JFrame {

    private JComboBox<String> comboDatasets;
    private JTextField campoK, campoRestarts;
    private JTextArea areaLog;
    private JButton btnAprender;

    // Datasets do enunciado (pág. 29)
    private final String[] datasets = {
            "bcancer.csv", "diabetes.csv", "hepatitis.csv",
            "parkisons.csv", "thyroid.csv", "soybean-large.csv", "satimage.csv"
    };

    public App1_Aprendizagem() {
        setTitle("Aplicação 1 - Aprendizagem e Gravação da Rede Bayesiana");
        setSize(700, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Painel de configuração
        JPanel painelConfig = new JPanel(new GridLayout(4, 2, 10, 10));
        painelConfig.setBorder(BorderFactory.createTitledBorder("Configuração"));

        painelConfig.add(new JLabel("Dataset:"));
        comboDatasets = new JComboBox<>(datasets);
        painelConfig.add(comboDatasets);

        painelConfig.add(new JLabel("Máximo de pais atributos (k):"));
        campoK = new JTextField("2", 5);
        painelConfig.add(campoK);

        painelConfig.add(new JLabel("Número de restarts aleatórios:"));
        campoRestarts = new JTextField("10", 5);  // Bom equilíbrio precisão/tempo
        painelConfig.add(campoRestarts);

        btnAprender = new JButton("Aprender e Gravar Rede (.bn)");
        btnAprender.setBackground(new Color(0, 128, 0));
        btnAprender.setForeground(Color.WHITE);
        painelConfig.add(new JLabel(""));
        painelConfig.add(btnAprender);

        // Área de log
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log da Execução"));

        add(painelConfig, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);

        btnAprender.addActionListener(e -> executarAprendizagem());

        log("Aplicação iniciada. Configure e clique em 'Aprender e Gravar Rede'.");
    }

    private void executarAprendizagem() {
        new Thread(() -> {
            try {
                String dataset = (String) comboDatasets.getSelectedItem();
                int k = Integer.parseInt(campoK.getText().trim());
                int restarts = Integer.parseInt(campoRestarts.getText().trim());

                if (k < 0 || restarts < 1) throw new IllegalArgumentException("Parâmetros inválidos.");

                log("\n>>> Carregando dataset: " + dataset);
                Amostra amostra = new Amostra(dataset);
                log(">>> Instâncias: " + amostra.length() + " | Atributos: " + amostra.dim());

                log(">>> Aprendizagem iniciada (k=" + k + ", restarts=" + restarts + ")");
                Grafosorientados grafo = Grafosorientados.grafoo(amostra.dim());
                grafo.aprender(amostra, k, restarts);

                log(">>> Construindo Rede Bayesiana (S=0.5)");
                Redebayesiana rede = new Redebayesiana(grafo, amostra, 0.5);

                String ficheiroBn = dataset.replace(".csv", ".bn");
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ficheiroBn))) {
                    oos.writeObject(rede);
                }

                log(">>> SUCESSO! Rede gravada em: " + ficheiroBn);
                log(">>> Pode agora usar a Aplicação 2 para classificar pacientes.");

            } catch (Exception ex) {
                log("ERRO: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
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