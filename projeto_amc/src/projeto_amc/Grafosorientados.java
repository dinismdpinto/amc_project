package projeto_amc;

import java.util.ArrayList;
import java.util.LinkedList;

public class Grafosorientados {
    int n; // Número de nós
    ArrayList<LinkedList<Integer>> adj; // Lista de adjacências

    public Grafosorientados(int n) {
        this.n = n;
        this.adj = new ArrayList<LinkedList<Integer>>(n);
        for (int i = 0; i < n; i++) {
            this.adj.add(new LinkedList<Integer>());
        }
    }
    
    // Construtor de cópia (útil para o algoritmo Hill Climbing)
    public Grafosorientados(Grafosorientados g) {
        this.n = g.n;
        this.adj = new ArrayList<LinkedList<Integer>>(n);
        for (int i = 0; i < n; i++) {
            // Copia a lista de cada nó
            LinkedList<Integer> originalList = g.adj.get(i);
            LinkedList<Integer> newList = new LinkedList<>();
            for(Integer val : originalList) {
                newList.add(val);
            }
            this.adj.add(newList);
        }
    }

    // Adicionar aresta: node1 -> node2
    public void add_edge(int node1, int node2) {
        // Verifica se já existe para não duplicar
        if (!adj.get(node1).contains(node2)) {
            adj.get(node1).add(node2);
        }
    }

    // Remover aresta: node1 -> node2
    public void remove_edge(int node1, int node2) {
        adj.get(node1).remove((Integer) node2);
    }

    // Inverter aresta: remove node1->node2 e cria node2->node1
    public void invert_edge(int node1, int node2) {
        remove_edge(node1, node2);
        add_edge(node2, node1);
    }
    
    // Retorna a lista de pais de um nó
    public LinkedList<Integer> parents(int node) {
        LinkedList<Integer> parentsList = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (adj.get(i).contains(node)) {
                parentsList.add(i);
            }
        }
        return parentsList;
    }

    // Verifica se existe ciclo no grafo (DFS)
    public boolean hasCycle() {
        boolean[] visited = new boolean[n];
        boolean[] recStack = new boolean[n]; // Pilha de recursão
        
        for (int i = 0; i < n; i++) {
            if (hasCycleUtil(i, visited, recStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleUtil(int i, boolean[] visited, boolean[] recStack) {
        if (recStack[i]) return true; // Se já está na pilha atual, é ciclo
        if (visited[i]) return false; // Se já foi visitado e não deu ciclo, ok

        visited[i] = true;
        recStack[i] = true;

        for (Integer neighbor : adj.get(i)) {
            if (hasCycleUtil(neighbor, visited, recStack)) {
                return true;
            }
        }

        recStack[i] = false; // Remove da pilha ao voltar
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(i).append(" -> ").append(adj.get(i)).append("\n");
        }
        return sb.toString();
    }
    public static void main(String[] args) {
        System.out.println("--- Teste de Grafos Orientados ---");
        
        // Criar um grafo com 5 nós (0 a 4)
        Grafosorientados g = new Grafosorientados(5);
        
        // Adicionar arestas (setas)
        g.add_edge(0, 1); // 0 aponta para 1
        g.add_edge(1, 2);
        g.add_edge(2, 3);
        
        System.out.println("Grafo Inicial:");
        System.out.println(g);
        
        // Testar ciclo (deve dar falso)
        System.out.println("Tem ciclo? " + g.hasCycle());
        
        // Forçar um ciclo (3 -> 1, fechando o loop 1-2-3-1)
        System.out.println("\nAdicionando aresta 3 -> 1 para criar ciclo...");
        g.add_edge(3, 1);
        System.out.println("Tem ciclo agora? " + g.hasCycle());
        
        // Remover aresta
        g.remove_edge(3, 1);
        System.out.println("Ciclo removido. Tem ciclo? " + g.hasCycle());
        
        System.out.println("\n--- Teste Concluído ---");
    }   
}