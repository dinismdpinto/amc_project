package projeto_amc;

import java.util.LinkedList;
import java.util.Queue;


public class Graphoo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    int n; // Número de nós
    LinkedList<Integer>[] adj; //Lista de adjacência (guarda os filhos)

    //Cria um grafo com n nós, mas sem nenhuma aresta
    @SuppressWarnings("unchecked")
    public Graphoo(int n) { 
        this.n = n;
        this.adj = new LinkedList[n]; 
        
        for (int i = 0; i < n; i++) {
            this.adj[i] = new LinkedList<Integer>(); // Acesso direto com [i]
        }
    }
    
   //Cria um novo grafo que é uma cópia exata do grafo original
    @SuppressWarnings("unchecked")
    public Graphoo(Graphoo g) { 
        this.n = g.n;
        this.adj = new LinkedList[n];
        
        for (int i = 0; i < n; i++) {
            // Copia a lista de cada nó
        	this.adj[i] = new LinkedList<>(g.adj[i]);
        	}
    }
    

    // adiciona ao grafo uma aresta de um nó para o outro
    public void add_edge(int node1, int node2) { // node1 (pai), node2 (filho) 
        if (!adj[node1].contains(node2)) { 
            adj[node1].add(node2);
        }
    }
    

    // Retira uma aresta
    public void remove_edge(int node1, int node2) {
        adj[node1].remove((Integer) node2); 
    }
    

    // Inverte a aresta no garfo
    public void invert_edge(int node1, int node2) {
        remove_edge(node1, node2);
        add_edge(node2, node1);
    }
    
    
    // Verifica se há um caminho (não vazio) de um nó para o outro (busca em largura)
    public boolean connected(int startNode, int endNode) {
        boolean[] visited = new boolean[n]; //Guarda os nós já visitados
        Queue<Integer> queue = new LinkedList<>(); //Fila que guarda a ordem de visita dos nós
        
        visited[startNode] = true; 
        queue.add(startNode); 
        
        while (!queue.isEmpty()) { // Enquanto existirem nós para analisar
            int curr = queue.poll(); // Retira o último nó da fila e guarda-o na variável 'curr' (current) 
            
            // Verifica vizinhos
            for (int neighbor : adj[curr]) { //Neighbor = filhos de curr
                if (neighbor == endNode) return true; // Encontrou caminho
                
                if (!visited[neighbor]) { // neighbor != endnode
                    visited[neighbor] = true;
                    queue.add(neighbor); // Adiciona para avaliar futuros caminhos
                }
            }
        }
        return false; // Não há caminho não nulo entre os nós
    }

    
    // Retorna a lista de pais de um nó
    public LinkedList<Integer> parents(int node) {
        LinkedList<Integer> parentsList = new LinkedList<>();
        
        for (int i = 0; i < n; i++) {
            if (adj[i].contains(node)) { 
                parentsList.add(i);
            }
        }
        return parentsList;
    }
    
    
     
    // Calcula a variação de MDL (Minimum description length) causada por uma operação 
    // Operações: 0 = remover; 1 = inverter; 2 = adicionar
    
    public double MDLdelta(Amostra a, int node1, int node2, int op) {
        double delta = 0.0; //variação 
        int maxParents = 3; //(k <= 2 + classe)

        // Remover aresta (node1 -> node2): node2 perde um pai; node1 não muda.
        if (op == 0) {
        	
        	//Verifica se a aresta não existe
        	if (!adj[node1].contains(node2)) return Double.NEGATIVE_INFINITY; // Operação inválida
        	
            double scoreBefore = scoreNode(node2, a);
            
            remove_edge(node1, node2); // Aplica temporariamente
            double scoreAfter = scoreNode(node2, a);
            add_edge(node1, node2);    // Reverte/ restaura grafo inicial
            
            delta = scoreAfter - scoreBefore;
        }
        
     // Inverter aresta (node1 -> node2 passa a ser node2 -> node1): node2 perde um pai; o node1 ganha um pai (ambos mudam)
        else if (op == 1) {
        	
        	if (!adj[node1].contains(node2)) return Double.NEGATIVE_INFINITY; // Node2 não é filho de node1

            // Para inverter, remove-se node1->node2 e adiciona-se node2->node1.
            // Tem de se verificar que node2->node1 não cria ciclo (grafo acíclico)
            remove_edge(node1, node2);
            boolean createsCycle = connected(node1, node2); // Verifica se ainda há conecção entre os dois nós
            add_edge(node1, node2); // Repor para calcular score inicial

            if (createsCycle) return Double.NEGATIVE_INFINITY; // Caso haja + que 1 connecção forma-se um grafo cíclico
            
            // Verificar limite de pais do node1 
            if (parents(node1).size() >= maxParents) return Double.NEGATIVE_INFINITY;
            
            
            double scoreBefore = scoreNode(node1, a) + scoreNode(node2, a);
            
            invert_edge(node1, node2); 
            double scoreAfter = scoreNode(node1, a) + scoreNode(node2, a);
            invert_edge(node2, node1); 
            
            delta = scoreAfter - scoreBefore;
        }
        
        // Inserir aresta (node1 -> node2): node2 ganha um pai; O node1 não muda.
        else if (op == 2) {
        	
        	// 1. Valida se já existe caminho node2 -> node1
            if (connected(node2, node1)) return Double.NEGATIVE_INFINITY;
            
            // 2. Valida limite de pais, porque o node2 vai ganhar um pai
            if (parents(node2).size() >= maxParents) return Double.NEGATIVE_INFINITY;
            
            // 3. Valida se o node1 já é pai do node2
            if (adj[node1].contains(node2)) return Double.NEGATIVE_INFINITY;
        	
            double scoreBefore = scoreNode(node2, a);
            
            add_edge(node1, node2);   
            double scoreAfter = scoreNode(node2, a);
            remove_edge(node1, node2); 
            
            delta = scoreAfter - scoreBefore;
        }
        
        

        return delta;
    }
    
    // ----------------------------------------------------------------
    // 3. Cálculo II (Interaction/ Mutual Information): Verosimilhança
    // Valor elevado: pais explicam o comportamento do nó
    // Valor baixo: pais e filhos independentes
    // ----------------------------------------------------------------
    
    double InteractionInformation(int i, Amostra a) {
        int classIndex = a.dim() - 1; // Nó da classe: última coluna
        LinkedList<Integer> parentsList = parents(i);
        double nrSamples = a.length(); // Número de exemplos/ linhas
        double sum = 0.0;
        
     // --- Caso 0 Pais ---
        if (parentsList.size() == 0) {
            return 0.0;
        }

     // --- Caso 1 Pai ---
        if (parentsList.size() == 1) {
            int parentNode = parentsList.get(0);

            // Loops com nomes claros (Child Value, Parent Value, Class Value)
            for (int childVal = 0; childVal < a.domain(i); childVal++) {
                for (int parentVal = 0; parentVal < a.domain(parentNode); parentVal++) {
                    for (int classVal = 0; classVal < a.domain(classIndex); classVal++) {
                        
                        // N(xi, pai, classe) -> Contagem conjunta dos 3
                        double countChildParentClass = a.count(i, parentNode, classIndex, childVal, parentVal, classVal);
                        
                        if (countChildParentClass != 0) {
                            // N(c) -> Contagem só da classe
                            double countClass = a.count(classIndex, classVal);               
                            // N(xi, c) -> Contagem Filho + Classe
                            double countChildClass = a.count(i, classIndex, childVal, classVal);      
                            // N(pai, c) -> Contagem Pai + Classe
                            double countParentClass = a.count(parentNode, classIndex, parentVal, classVal);    
                            
                            // Fórmula
                            double term = countChildParentClass * countClass / (countChildClass * countParentClass);
                            
                            // Acumula o resultado na variável 's'
                            sum = sum + countChildParentClass * log2(term);
                        }
                    }
                }
            }
        }

        // --- Caso 2 Pais ---
        if (parentsList.size() == 2) {
            int parent1 = parentsList.get(0);
            int parent2 = parentsList.get(1);

            for (int childVal = 0; childVal < a.domain(i); childVal++) {
                for (int p1Val = 0; p1Val < a.domain(parent1); p1Val++) {      
                    for (int p2Val = 0; p2Val < a.domain(parent2); p2Val++) {  
                        for (int classVal = 0; classVal < a.domain(classIndex); classVal++) {
                            
                            // Contagem conjunta total (Filho + Pai1 + Pai2 + Classe)
                            double countChildBothParentsClass = a.count(i, parent1, parent2, classIndex, childVal, p1Val, p2Val, classVal);
                            
                            if (countChildBothParentsClass != 0) {
                                double countClass = a.count(classIndex, classVal);
                                
                                double countChildClass = a.count(i, classIndex, childVal, classVal); 
                                
                                // Contagem dos dois pais com a classe
                                double countBothParentsClass = a.count(parent1, parent2, classIndex, p1Val, p2Val, classVal); 

                                // Fórmula adaptada
                                double term = countChildBothParentsClass * countClass / (countChildClass * countBothParentsClass);
                                sum = sum + countChildBothParentsClass * log2(term);
                            }
                        }
                    }
                }
            }
        }
        
        // Retorna a soma 's' dividida pelo número de exemplos 'nrSamples'
        return sum / nrSamples;   
    }
    
    // ---------------------------------------------------------------
    // 3. ScoreNode: Combina Verosimilhança e Penalização
    // Fórmula (Maximizar): MDLs = LL - Penalização (complexidade)
    // Só aceita um novo pai se o ganho de informação for superior ao "custo" de aumento da complexidade
    // ---------------------------------------------------------------
    private double scoreNode(int i, Amostra a) {
    	
        // LL: Verosimilhança; 
    	// Mede o quão bem a rede descreve os dados
        double LL = a.length() * InteractionInformation(i, a); 

        int classIndex = a.dim() - 1; // Última coluna corresponde à variável da classe
        LinkedList<Integer> parentsList = parents(i);
        
        // Cálculo da Penalização (Complexidade)
        double domainParents = 1.0;
        for (int p : parentsList) { // Para cada p na parentsList
            domainParents *= a.domain(p); // Produto do domínio dos pais (nr de valores que os vários p's podem assumir)
        }

        // Número de parâmetros livres na tabela de probabilidade condicional
        double params = (a.domain(i) - 1) * domainParents * a.domain(classIndex);
        
        // Penalidade MDL padrão
        // Penaliza estruturas complexas para evitar overfitting
        double penalty = (params * log2(a.length())) / 2.0;

        return LL - penalty; // Obj: Maximizar valor 
    }
    
    
    // Calcula o score MDL (Minimum description length) total da amostra 
    public double MDL(Amostra a) {
        double totalMDL = 0.0;
        
        //Assume-se que a classe é a última variável e não se calcula os pais dela
        for (int i = 0; i < a.dim() - 1; i++) {
            totalMDL += scoreNode(i, a); // soma do scoreNode de todos os nós 
        }
        return totalMDL;
    }

    
    
    private double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
    

    // Apresentação o grafo na consola 
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph [n=" + n + "]\n"); // Indica nr total de nós
        for (int i = 0; i < n; i++) {
        	sb.append("Node " + i + " points to -> " + adj[i] + "\n"); // Associa cada nó à sua lista de adjacência
        }
        return sb.toString();
    }
}