package projeto_amc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class Amostra implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    // Lista temporária para leitura
    private ArrayList<int[]> bufferLeitura;
   
    // Acesso direto à memória (Rápido e Leve)
    private int[][] dados;
   
    protected int[] maximos = null;
   
    public Amostra() {
        this.bufferLeitura = new ArrayList<>();
    }
    
    public Amostra(String csvFile) {
        this();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(csvFile));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                add(convert(line));
            }
            finalizarLeitura(); // Converte para matriz fixa
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) try { br.close(); } catch (IOException e) {}
        }
    }
    
    private void finalizarLeitura() {
        if (bufferLeitura != null && !bufferLeitura.isEmpty()) {
            dados = new int[bufferLeitura.size()][];
            bufferLeitura.toArray(dados);
            bufferLeitura = null;
        }
    }
    
    public Amostra amostraSem(int index) {
        Amostra nova = new Amostra();
       
        if (this.dados == null && this.bufferLeitura != null) finalizarLeitura();
        if (this.maximos != null) nova.maximos = this.maximos.clone();
        int N = this.length();
        nova.dados = new int[N - 1][];
        if (index > 0) {
            System.arraycopy(this.dados, 0, nova.dados, 0, index);
        }
        if (index < N - 1) {
            System.arraycopy(this.dados, index + 1, nova.dados, index, N - index - 1);
        }
        return nova;
    }
    
    static int[] convert(String line) {
        String[] strings = line.split(",");
        int[] vec = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            vec[i] = Integer.parseInt(strings[i].trim());
        }
        return vec;
    }
    
    public void add(int[] v) {
        if (dados != null) {
            if (bufferLeitura == null) {
                bufferLeitura = new ArrayList<>();
                for(int[] row : dados) bufferLeitura.add(row);
            }
            dados = null;
        }
        if (bufferLeitura == null) bufferLeitura = new ArrayList<>();
        if (maximos == null) {
            maximos = new int[v.length];
            System.arraycopy(v, 0, maximos, 0, v.length);
        } else {
            for (int i = 0; i < maximos.length; i++)
                if (maximos[i] < v[i]) maximos[i] = v[i];
        }
        bufferLeitura.add(v);
    }
    
    public int length() {
        if (dados != null) return dados.length;
        return bufferLeitura.size();
    }
    
    public int dim() {
        if (maximos == null) return 0;
        return maximos.length;
    }
    
    public int[] element(int i) {
        if (dados != null) return dados[i];
        return bufferLeitura.get(i);
    }
    
    public int count(int[] var, int[] val) {
        if (dados == null) finalizarLeitura();
        int r = 0;
        int rows = dados.length;
        int numVars = var.length;
        for (int j = 0; j < rows; j++) {
            int[] linha = dados[j];
            boolean match = true;
            for (int k = 0; k < numVars; k++) {
                if (linha[var[k]] != val[k]) {
                    match = false;
                    break;
                }
            }
            if (match) r++;
        }
        return r;
    }
    
    // Overloads
    public int count(int var, int val) {
        if (dados == null) finalizarLeitura();
        int r = 0;
        for (int[] linha : dados) {
            if (linha[var] == val) r++;
        }
        return r;
    }
    
    public int count(int var1, int var2, int val1, int val2) {
        if (dados == null) finalizarLeitura();
        int r = 0;
        for (int[] linha : dados) {
            if (linha[var1] == val1 && linha[var2] == val2) r++;
        }
        return r;
    }
    
    public int count(int var1, int var2, int var3, int val1, int val2, int val3){
        if (dados == null) finalizarLeitura();
        int r = 0;
        for (int[] linha : dados) {
            if (linha[var1] == val1 && linha[var2] == val2 && linha[var3] == val3) r++;
        }
        return r;
    }
    
    public int count(int var1, int var2, int var3, int var4, int val1, int val2, int val3, int val4){
        if (dados == null) finalizarLeitura();
        int r = 0;
        for (int[] linha : dados) {
            if (linha[var1] == val1 && linha[var2] == val2 && linha[var3] == val3 && linha[var4] == val4) r++;
        }
        return r;
    }
    
    public int domain(int var[]) {
        if (var == null) return 0;
        int r = 1;
        for (int i : var) r *= (maximos[i] + 1);
        return r;
    }
    
    public int domain(LinkedList<Integer> var){
        if (var == null) return 0;
        int r = 1;
        for (int i : var) r *= (maximos[i] + 1);
        return r;
    }
    
    public int domain(int v) { return maximos[v] + 1; }
   
    @Override
    public String toString() {
        return "Amostra (Sem Cache) com " + length() + " elementos.";
    }
}