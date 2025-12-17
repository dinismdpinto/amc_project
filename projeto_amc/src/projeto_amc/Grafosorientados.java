package projeto_amc;

import java.util.ArrayList;
import java.util.LinkedList;

public class Grafosorientados {

    int n; 
    ArrayList<LinkedList<Integer>> adj; 

    public Grafosorientados(int n) {
        this.n = n;
        this.adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            adj.add(new LinkedList<>());
    }

    /* ================= OPERAÇÕES BÁSICAS ================= */
    
    public void add_edge(int u, int v) {
        if (!adj.get(u).contains(v))
            adj.get(u).add(v);
    }

    public void remove_edge(int u, int v) {
        adj.get(u).remove((Integer) v);
    }

    public void invert_edge(int u, int v) {
        remove_edge(u, v);
        add_edge(v, u);
    }

    public boolean connected(int u, int v) {
        boolean[] visited = new boolean[n];
        return dfsConnected(u, v, visited);
    }

    private boolean dfsConnected(int cur, int target, boolean[] visited) {
        if (cur == target) return true;
        visited[cur] = true;
        for (int nxt : adj.get(cur))
            if (!visited[nxt] && dfsConnected(nxt, target, visited))
                return true;
        return false;
    }

    public LinkedList<Integer> parents(int node) {
        LinkedList<Integer> p = new LinkedList<>();
        for (int i = 0; i < n; i++)
            if (adj.get(i).contains(node))
                p.add(i);
        return p;
    }

    /* ========================================================== */
    /* RESPOSTA 2 e 3: CÁLCULO OTIMIZADO DE MDL         */
    /* ========================================================== */

    /**
     * Calcula o score MDL total da rede.
     * Implementa a Eq 2.6.3 (Maximização).
     */
    public double MDL(Amostra T) {
        double totalScore = 0.0;
        
        // O score total é a soma dos scores locais de cada nó
        for (int i = 0; i < n; i++) {
            totalScore += scoreLocal(i, T);
        }
        
        // Nota: A constante global (|Dc|-1) da penalização cancela-se na comparação,
        // mas se quiseres o valor exato absoluto, podes subtrair aqui.
        // Para o algoritmo Greedy, a soma dos locais é suficiente.
        return totalScore;
    }

    /**
     * Calcula a variação de MDL para uma operação.
     * Otimização: Calcula apenas a diferença nos nós afetados.
     * Verificação de Ciclos: Usa connected() localmente.
     * * @param operacao 0: remover, 1: inverter, 2: adicionar
     * @return delta (positivo significa melhoria) ou -Infinity se inválido (ciclo)
     */
    public double MDLdelta(Amostra T, int u, int v, int operacao) {
        double delta = Double.NEGATIVE_INFINITY;

        // Caso 2: ADICIONAR aresta u -> v
        // Nós afetados: apenas v (mudam os pais de v)
        if (operacao == 2) {
            // 1. Verificação de Ciclo: Se já existe caminho v -> ... -> u, adicionar u->v cria ciclo.
            if (connected(v, u)) return Double.NEGATIVE_INFINITY; 

            double scoreAntes = scoreLocal(v, T);
            
            add_edge(u, v); // Aplica
            double scoreDepois = scoreLocal(v, T);
            remove_edge(u, v); // Desfaz
            
            delta = scoreDepois - scoreAntes;
        }
        
        // Caso 0: REMOVER aresta u -> v
        // Nós afetados: apenas v
        else if (operacao == 0) {
            // Remover arestas nunca cria ciclos, não precisa verificar.
            double scoreAntes = scoreLocal(v, T);
            
            remove_edge(u, v); // Aplica
            double scoreDepois = scoreLocal(v, T);
            add_edge(u, v); // Desfaz
            
            delta = scoreDepois - scoreAntes;
        }
        
        // Caso 1: INVERTER aresta u -> v para v -> u
        // Nós afetados: u e v (ambos mudam de pais)
        else if (operacao == 1) {
            // 1. Verificar Ciclo:
            // Para inverter u->v, primeiro removemos u->v.
            // Depois verificamos se adicionar v->u cria ciclo (ou seja, se existe caminho u->...->v).
            remove_edge(u, v);
            boolean criaCiclo = connected(u, v);
            add_edge(u, v); // Repor para calcular o score "Antes"
            
            if (criaCiclo) return Double.NEGATIVE_INFINITY;

            double scoreAntes = scoreLocal(u, T) + scoreLocal(v, T);
            
            invert_edge(u, v); // Aplica
            double scoreDepois = scoreLocal(u, T) + scoreLocal(v, T);
            invert_edge(v, u); // Desfaz (inverte de volta)
            
            delta = scoreDepois - scoreAntes;
        }

        return delta;
    }

    /**
     * Método Auxiliar: Calcula o score MDL de UM único nó (Xi).
     * Score_i = m * I(Xi; Pais) - Penalização_i
     */
    private double scoreLocal(int Xi, Amostra T) {
        int m = T.length();
        
        // 1. Informação Mútua Condicional (Log-Likelihood Term)
        double imc = informacaoMutuaCondicional(T, Xi); // Método já existente (ajustar se necessário)
        double logLikelihood = m * imc;

        // 2. Penalização (Complexity Term) [cite: 183]
        // |Theta_i| = (Dim(Xi) - 1) * q_i * Dim(C)
        // Onde q_i é o produto das dimensões dos pais
        
        int C = T.dim() - 1; // Índice da classe
        int dimXi = T.domain(Xi);
        int dimC = T.domain(C);
        
        int qi = 1;
        for (int pai : parents(Xi)) {
            qi *= T.domain(pai);
        }
        
        int paramsLocais = (dimXi - 1) * qi * dimC;
        
        // Penalização = (log2(m) / 2) * |Theta_i|
        double penalizacao = (Math.log(m) / Math.log(2)) * paramsLocais / 2.0;

        // MDL Score (para maximizar) = LL - Penalização
        return logLikelihood - penalizacao;
    }

    /* ================= MÉTODOS DE CÁLCULO (IMC) ================= */
    // Mantém a tua lógica de percorrimento recursivo, apenas certifica-te 
    // que informacaoMutuaCondicional chama o helper corretamente.

    private double informacaoMutuaCondicional(Amostra T, int Xi) {
        int C = T.dim() - 1;              
        LinkedList<Integer> pais = parents(Xi);
        int m = T.length();

        // Constrói vetor: [Pais..., Xi, C]
        int[] vars = new int[pais.size() + 2];
        for (int i = 0; i < pais.size(); i++)
            vars[i] = pais.get(i);

        vars[pais.size()]     = Xi;
        vars[pais.size() + 1] = C;

        int[] vals = new int[vars.length];

        return percorreIMC(T, vars, vals, 0, m);
    }

    // Este método mantém-se igual ao que enviaste, pois a lógica recursiva está correta.
    private double percorreIMC(Amostra T, int[] vars, int[] vals, int idx, int m) {
        if (idx == vars.length) {
            double n_xyz = T.count(vars, vals);
            if (n_xyz == 0) return 0.0;

            int Xi = vars[vars.length - 2];
            int C  = vars[vars.length - 1];

            // Subconjuntos para calcular P(Xi, C) e P(Pais, C)
            int[] vars_xc = { Xi, C };
            int[] vals_xc = { vals[vars.length - 2], vals[vars.length - 1] };

            int[] vars_wc = new int[vars.length - 1];
            int[] vals_wc = new int[vals.length - 1];
            System.arraycopy(vars, 0, vars_wc, 0, vars.length - 1);
            System.arraycopy(vals, 0, vals_wc, 0, vals.length - 1);

            double n_xc = T.count(vars_xc, vals_xc);
            double n_wc = T.count(vars_wc, vals_wc);

            // Probabilidades empíricas
            double P_xyz = n_xyz / m;
            double P_xc  = n_xc  / m;
            double P_wc  = n_wc  / m;
            double P_c   = (double)T.count(C, vals[vars.length-1]) / m;

            // Fórmula IMC: P(x,w,c) * log( (P(x,w,c)*P(c)) / (P(x,c)*P(w,c)) ) [cite: 137]
            // Nota: P(c) está no numerador dentro do logaritmo
            return P_xyz * Math.log((P_xyz * P_c) / (P_xc * P_wc));
        }

        double sum = 0.0;
        // v percorre o domínio da variável vars[idx]
        for (int v = 0; v < T.domain(vars[idx]); v++) {
            vals[idx] = v;
            sum += percorreIMC(T, vars, vals, idx + 1, m);
        }
        return sum;
    }
}