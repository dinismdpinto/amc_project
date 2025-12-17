package projeto_amc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class Amostra {
	ArrayList<int []> lista;  // Lista que guarda os dados (matriz m x n)
	private int [] maximos = null;  // Vetor auxiliar para guardar o valor máximo encontrado em cada variável

	public Amostra() { //construtor vazio
		this.lista = new ArrayList<int []>();
	}

	//Converte a string num vetor de inteiros 
	static int [] convert (String line) {
		String cvsSplitBy = ",";
		String[] strings     = line.split(cvsSplitBy); // Cria um vetor de texto com uma nova entrada sempre que encontra uma vírgula na string introduzida
		int[] stringToIntVec = new int[strings.length]; //Cria um vetor de números inteiros vazio com o mesmo tamanho do vetor de texto
		for (int i = 0; i < strings.length; i++)
			stringToIntVec[i] = Integer.parseInt(strings[i]); //Transforma cada entrada do vetor de txto no número inteiro correspondente
		return stringToIntVec;
		}
	
	
	//Carrega a amostra a partir de um ficheiro CSV
	public Amostra(String csvFile) {
		this.lista = new ArrayList<int []>();;
		this.maximos = null; 
		BufferedReader br = null;
		String line = "";
		

		try {br = new BufferedReader(new FileReader(csvFile));
		
			//Lê o ficheiro linha a linha, converte o texto em inteiros e guarda na amostra
			while ((line = br.readLine()) != null) {  
				add(convert(line)); //guarda a linha do documento na variável line e tranforma-la em inteiros
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace(); // Mostra erro se o ficheiro não existir 
		} catch (IOException e) {
			e.printStackTrace(); // Mostra erro se falhar a leitura
		} finally {
			if (br != null) {
				try {
					br.close(); //Fecha o ficheiro 
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Retorna o número de variáveis (colunas) da amostra
	public int dim() {
		return maximos.length;
	}
	
	
	// Adiciona um vetor à amostra e atualiza os valores máximos (add)
	public void add (int[] v){ 
		
		// Se for o primeiro elemento, inicializa os máximos
		if (maximos == null) {
			maximos = new int[v.length];
			System.arraycopy(v, 0, maximos, 0, v.length);
			}
		
		// Atualiza o valor máximo
		else {
			for (int i = 0; i < maximos.length; i++)
				if (maximos[i] < v[i]) maximos[i] = v[i];
		}
		lista.add(v);
	}
	
	// Retorna o comprimento da amostra 
	public int length (){
		return lista.size();
	}
	
	// Recebe uma posição e retorna o vetor da amostra
	public int [] element (int i){
		return lista.get(i);
	}
	
	
	//Verifica se a linha 'j' tem os valores 'val' nas variáveis 'var' indicadas
	private boolean equalQ(int[] var, int[] val, int j){
		boolean r = true;
		if (var != null) {
		int v[] = lista.get(j); // Obtém a linha j
		
		// Verifica cada variável pedida
		for (int i=0; i < var.length && r; i++)
			if (v[var[i]]!=val[i]) r=false; // Se diferir, retorna falso
		}
		
		return r;
	}
	
	
	// Retorna o número de linhas que satisfazem essa condição
	public int count(int[] var, int[] val){
		int r = 0;
		for (int j = 0; j < lista.size(); j++)
			if (equalQ(var,val,j)) r+=1;
		return r;
	}
	
	// Retorna o número de ocorrências desses valores para essas variáveis na amostra
	public int domain (int var[]){
		if (var == null)
			return 0;
		else {
		int r = 1;
		for (int i = 0; i < var.length; i++)
			r *= (maximos[var[i]]+1);
		return r;
		}
	}
	
	public int domain (LinkedList<Integer> var){
		if (var == null)
			return 0;
		else {
		int r=1;
		for (int i=0; i<var.size();i++)
			r*=(maximos[var.get(i)]+1);
		return r;
		}
	}
	
	//Retorna o número de valores possíveis de uma única variável
	public int domain (int v){
		return maximos[v] + 1;
		}
	
	// Conta ocorrências para 1 variável
	public int count(int var, int val){
		int r=0;
		int vars[] = {var};
		int vals[] = {val};
		for (int j = 0; j < lista.size(); j++)
			if (equalQ(vars,vals,j)) r+=1;
		return r;
	}
	
	// Conta ocorrências para 2 variáveis
	public int count(int var1, int var2, int val1, int val2){
		int r=0;
		int vars[] = {var1,var2};
		int vals[] = {val1,val2};
		for (int j = 0; j < lista.size(); j++)
			if (equalQ(vars,vals,j)) r+=1;
		return r;
	}
	
	// Conta ocorrências para 3 variáveis
	public int count(int var1, int var2, int var3, int val1, int val2, int val3){
		int r=0;
		int vars[] = {var1,var2,var3};
		int vals[] = {val1,val2,val3};
		for (int j = 0; j < lista.size(); j++)
			if (equalQ(vars,vals,j)) r+=1;
		return r;
	}
	
	// Conta ocorrências para 4 variáveis
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
		String s="\n[\n";
		if (lista.size()>0) s+=Arrays.toString(lista.get(0));
		for (int i=1; i<lista.size();i++)
			s+="\n"+Arrays.toString(lista.get(i));
		s+="\n]";
		int [] mydomain= new int[maximos.length];
		for (int i=0; i<maximos.length;i++)
			mydomain[i]=maximos[i]+1;
			
		return "Domínios    \n" + Arrays.toString(mydomain)+"\n"+"Amostra " + s;
	}

	public static void main(String[] args) {
		Amostra amostra = new Amostra("bcancer.csv");
		System.out.println(amostra);
		
		int vars[][] = new int [5][];
		int vals[][] = new int [5][];
		
		vars[0] = new int[] {0,3,2,10};
		vals[0] = new int[] {1,3,2,1};
		
		vars[1] = new int[] {2,3,5,10};
		vals[1] = new int[] {1,2,1,1};
		
		vars[2] = new int[] {0,6,7,9};
		vals[2] = new int[] {0,0,1,0};
		
		vars[3] = new int[] {3,4,5,9};
		vals[3] = new int[] {3,2,0,1};
		
		vars[4] = new int[] {6,2,3,10};
		vals[4] = new int[] {1,0,2,0};
		
		for(int i = 0; i < vars.length; i++) {
			System.out.println(amostra.count(vars[i], vals[i]));
		}
		
		
	}

}
