package projeto_amc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class Amostra {
    
    // Lista que guarda os dados (matriz m x n)
    ArrayList<int[]> lista;  
    
    // Vetor auxiliar para guardar o valor máximo encontrado em cada variável (define o domínio).
    protected int[] maximos = null;  

    // Construtor vazio
    public Amostra() { 
        this.lista = new ArrayList<int []>();
    }

    // --- NOVO MÉTODO (ESSENCIAL PARA O LEAVE-ONE-OUT) ---
    // Cria uma nova Amostra excluindo apenas a linha na posição 'index'.
    // Usamos isto para: Treinar com (N-1) casos e Testar com o caso 'index'.
    public Amostra amostraSem(int index) {
        Amostra nova = new Amostra();
        
        // Copia os domínios (maximos) da amostra original para a nova.
        if (this.maximos != null) {
            nova.maximos = this.maximos.clone();
        }

        // Copia todos os elementos exceto o que está na posição 'index'
        for (int i = 0; i < this.lista.size(); i++) {
            if (i == index) continue; // Pula o paciente de teste
            nova.lista.add(this.lista.get(i));
        }
        return nova;
    }
    // -----------------------------------------------------

    // Converte a string num vetor de inteiros 
    static int[] convert(String line) {
        String cvsSplitBy = ",";
        // Cria um vetor de texto com uma nova entrada sempre que encontra uma vírgula
        String[] strings = line.split(cvsSplitBy); 
        
        int[] stringToIntVec = new int[strings.length]; 
        for (int i = 0; i < strings.length; i++) {
            stringToIntVec[i] = Integer.parseInt(strings[i].trim()); 
        }
        return stringToIntVec;
    }
    
    // Carrega a amostra a partir de um ficheiro CSV
    public Amostra(String csvFile) {
        this.lista = new ArrayList<int []>();
        this.maximos = null; 
        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(csvFile));
        
            // Lê o ficheiro linha a linha
            while ((line = br.readLine()) != null) {  
                // Adicionei verificação para não ler linhas vazias no fim do ficheiro
                if (line.trim().isEmpty()) continue;
                add(convert(line)); 
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace(); 
        } catch (IOException e) {
            e.printStackTrace(); 
        } finally {
            if (br != null) {
                try {
                    br.close(); 
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Retorna o número de variáveis (colunas) da amostra
    public int dim() {
        if (maximos == null) return 0;
        return maximos.length;
    }
    
    // Adiciona um vetor à amostra e atualiza os valores máximos (add)
    public void add(int[] v){ 
        // Se for o primeiro elemento, inicializa os máximos
        if (maximos == null) {
            maximos = new int[v.length];
            System.arraycopy(v, 0, maximos, 0, v.length);
        }
        // Atualiza o valor máximo se encontrarmos um maior
        else {
            for (int i = 0; i < maximos.length; i++)
                if (maximos[i] < v[i]) maximos[i] = v[i];
        }
        lista.add(v);
    }
    
    // Retorna o comprimento da amostra (número de linhas)
    public int length(){
        return lista.size();
    }
    
    // Recebe uma posição e retorna o vetor da amostra
    public int[] element(int i){
        return lista.get(i);
    }
    
    // Verifica se a linha 'j' tem os valores 'val' nas variáveis 'var' indicadas
    private boolean equalQ(int[] var, int[] val, int j){
        boolean r = true;
        if (var != null) {
            int v[] = lista.get(j); // Obtém a linha j
            
            // Verifica cada variável pedida
            for (int i=0; i < var.length && r; i++)
                if (v[var[i]] != val[i]) r = false; 
        }
        return r;
    }
    
    // Retorna o número de linhas que satisfazem a condição (count genérico)
    public int count(int[] var, int[] val){
        int r = 0;
        for (int j = 0; j < lista.size(); j++)
            if (equalQ(var,val,j)) r+=1;
        return r;
    }
    
    // Calcula o tamanho do espaço de estados para um conjunto de variáveis
    public int domain(int var[]){
        if (var == null) return 0;
        else {
            int r = 1;
            for (int i = 0; i < var.length; i++)
                r *= (maximos[var[i]] + 1);
            return r;
        }
    }
    
    public int domain(LinkedList<Integer> var){
        if (var == null) return 0;
        else {
            int r = 1;
            for (int i = 0; i < var.size(); i++)
                r *= (maximos[var.get(i)] + 1);
            return r;
        }
    }
    
    // Retorna o número de valores possíveis de uma única variável (Dominio)
    public int domain(int v){
        return maximos[v] + 1;
    }
    
    // Sobrecargas do count (mantive as tuas originais, estão corretas)
    public int count(int var, int val){
        int r=0;
        int vars[] = {var};
        int vals[] = {val};
        for (int j = 0; j < lista.size(); j++)
            if (equalQ(vars,vals,j)) r+=1;
        return r;
    }
    
    public int count(int var1, int var2, int val1, int val2){
        int r=0;
        int vars[] = {var1,var2};
        int vals[] = {val1,val2};
        for (int j = 0; j < lista.size(); j++)
            if (equalQ(vars,vals,j)) r+=1;
        return r;
    }
    
    public int count(int var1, int var2, int var3, int val1, int val2, int val3){
        int r=0;
        int vars[] = {var1,var2,var3};
        int vals[] = {val1,val2,val3};
        for (int j = 0; j < lista.size(); j++)
            if (equalQ(vars,vals,j)) r+=1;
        return r;
    }
    
    public int count(int var1, int var2, int var3, int var4, int val1, int val2, int val3, int val4){
        int r=0;
        int vars[] = {var1,var2,var3,var4};
        int vals[] = {val1,val2,val3,val4};
        for (int j = 0; j < lista.size(); j++)
            if (equalQ(vars,vals,j)) r+=1;
        return r;
    }
    
    @Override
    public String toString() {
        // --- ALTERAÇÃO AQUI: REMOVIDA A LIMITAÇÃO DAS 10 LINHAS ---
        String s = "\n[\n";
        
        // Agora percorre a lista inteira até ao fim (lista.size())
        if (lista.size() > 0) {
            s += Arrays.toString(lista.get(0));
        }
        for (int i = 1; i < lista.size(); i++) {
            s += "\n" + Arrays.toString(lista.get(i));
        }
        s += "\n]";
        
        int[] mydomain = new int[maximos.length];
        for (int i = 0; i < maximos.length; i++) {
            mydomain[i] = maximos[i] + 1;
        }
            
        return "Domínios (Tamanhos): \n" + Arrays.toString(mydomain) + "\n" + "Amostra (Completa): " + s;
    }

    public static void main(String[] args) {
        Amostra amostra = new Amostra("bcancer.csv");
        System.out.println(amostra);
    }
}