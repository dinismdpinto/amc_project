package projeto_amc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.HashMap; // <--- NOVO IMPORT PARA OTIMIZAÇÃO

public class Amostra {
    
    // Lista que guarda os dados (matriz m x n)
    ArrayList<int[]> lista;  
    
    // Vetor auxiliar para guardar o valor máximo encontrado em cada variável (define o domínio).
    protected int[] maximos = null;  

    // --- OTIMIZAÇÃO (CACHE) ---
    // Guarda as contagens já feitas para não ter de percorrer a lista toda outra vez.
    // Chave (String): Representa a pergunta (ex: "vars:[0,1]|vals:[1,1]")
    // Valor (Integer): A resposta (ex: 50 ocorrências)
    private HashMap<String, Integer> cache;

    // Construtor vazio
    public Amostra() { 
        this.lista = new ArrayList<int []>();
        this.cache = new HashMap<>(); // Inicializa o cache vazio
    }

    // --- MÉTODO LEAVE-ONE-OUT ---
    public Amostra amostraSem(int index) {
        Amostra nova = new Amostra();
        
        // Copia os domínios
        if (this.maximos != null) {
            nova.maximos = this.maximos.clone();
        }

        // Copia os dados (exceto o índice removido)
        for (int i = 0; i < this.lista.size(); i++) {
            if (i == index) continue; 
            nova.lista.add(this.lista.get(i));
        }
        
        // Nota: Não copiamos o 'cache' antigo porque ao remover uma linha,
        // as contagens mudam. A 'nova' amostra começa com o cache limpo.
        return nova;
    }

    // Converte a string num vetor de inteiros 
    static int[] convert(String line) {
        String cvsSplitBy = ",";
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
        this.cache = new HashMap<>(); // Inicializa o cache vazio
        
        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {  
                if (line.trim().isEmpty()) continue;
                add(convert(line)); 
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace(); 
        } catch (IOException e) {
            e.printStackTrace(); 
        } finally {
            if (br != null) {
                try { br.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    public int dim() {
        if (maximos == null) return 0;
        return maximos.length;
    }
    
    public void add(int[] v){ 
        if (maximos == null) {
            maximos = new int[v.length];
            System.arraycopy(v, 0, maximos, 0, v.length);
        }
        else {
            for (int i = 0; i < maximos.length; i++)
                if (maximos[i] < v[i]) maximos[i] = v[i];
        }
        lista.add(v);
        // Nota: Se adicionamos dados novos, o cache antigo pode ficar inválido.
        // Como no projeto carregamos tudo no início e não mudamos mais, não há problema.
        // Se fosse dinâmico, teríamos de fazer cache.clear() aqui.
    }
    
    public int length(){
        return lista.size();
    }
    
    public int[] element(int i){
        return lista.get(i);
    }
    
    private boolean equalQ(int[] var, int[] val, int j){
        boolean r = true;
        if (var != null) {
            int v[] = lista.get(j); 
            for (int i=0; i < var.length && r; i++)
                if (v[var[i]] != val[i]) r = false; 
        }
        return r;
    }
    
    // --- OTIMIZAÇÃO PRINCIPAL AQUI ---
    // Retorna o número de linhas que satisfazem a condição
    public int count(int[] var, int[] val){
        // 1. Criar uma chave única para esta pesquisa
        // Arrays.toString cria algo como "[0, 2]"
        String chave = Arrays.toString(var) + "|" + Arrays.toString(val);
        
        // 2. Verificar se já calculámos isto antes
        if (cache.containsKey(chave)) {
            // Se sim, devolve logo (Instantâneo!)
            return cache.get(chave);
        }

        // 3. Se não, temos de contar linha a linha (Lento)
        int r = 0;
        for (int j = 0; j < lista.size(); j++)
            if (equalQ(var,val,j)) r+=1;
        
        // 4. Guardar o resultado no Cache para a próxima vez
        cache.put(chave, r);
        
        return r;
    }
    
    // Restantes métodos domain e overloads do count mantêm-se iguais
    // (Os overloads chamam o count principal, por isso também beneficiam do cache)

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
    
    public int domain(int v){
        return maximos[v] + 1;
    }
    
    public int count(int var, int val){
        int vars[] = {var};
        int vals[] = {val};
        return count(vars, vals); // Chama o count otimizado
    }
    
    public int count(int var1, int var2, int val1, int val2){
        int vars[] = {var1,var2};
        int vals[] = {val1,val2};
        return count(vars, vals);
    }
    
    public int count(int var1, int var2, int var3, int val1, int val2, int val3){
        int vars[] = {var1,var2,var3};
        int vals[] = {val1,val2,val3};
        return count(vars, vals);
    }
    
    public int count(int var1, int var2, int var3, int var4, int val1, int val2, int val3, int val4){
        int vars[] = {var1,var2,var3,var4};
        int vals[] = {val1,val2,val3,val4};
        return count(vars, vals);
    }
    
    @Override
    public String toString() {
        String s = "\n[\n";
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
            
        return "Domínios: \n" + Arrays.toString(mydomain) + "\n" + "Amostra: " + s;
    }

    public static void main(String[] args) {
        Amostra amostra = new Amostra("bcancer.csv");
        System.out.println(amostra);
    }
}