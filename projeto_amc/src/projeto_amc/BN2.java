package projeto_amc;

import java.util.LinkedList;

public class BN2 {
	Amostra a;
	Grafosorientados g;
	double S;


	public BN2(Amostra a, Grafosorientados g, double s) {
		super();
		this.a = a;
		this.g = g;
		S = s;
	}

	@Override
	public String toString() {
		return "BN2 [a=" + a + ", S=" + S + "]";
	}
	double theta(int i, int v[]) {
		LinkedList<Integer> pais = g.parents(1);
		if (pais.size()==0) {
			return (a.count(i,v[i])+S)/(1+S*a.domain(i));
		}
		if (pais.size()==1) {
			int pai=pais.getFirst();
			return ((a.count(i, pai, v[i],v[pai]))+S)/(a.count(pai, v[pai])+S*a.domain(i));
		}
		if (pais.size()==2) {
			int pai1=pais.getFirst();
			int pai2=pais.getLast();
			return (a.count(i, pai1, pai2,v[i],v[pai1],v[pai2])+S)/(a.count(pai1, pai2, v[pai1],v[pai2])+S*a.domain(i));
		}
		return 0;
	}//Otimizamos guardando os valores calculados e depois vamos buscar à estrutura (ex: hash tables; hashing)
	
	public double prob(int v[]) {
		int classe=v.length-1;
		double p=(double)a.count(classe, v[classe])/a.length();
		for (int i=0; i<classe; i++)
			p=p*theta(i,v);
		return p;
	}
	public int classify(int  m[]) {//Sem a classe
		int classe=a.dim()-1;
		int na[]=new int[m.length+1];
		for(int i=0;i<m.length;i++)
			na[i]=m[i];
		double pmax=0;
		int mc=-1;
		for (int c=0;c<a.domain(classe);c++) {
			if(prob(na)>pmax) {
				pmax=prob(na);
				mc=c;
			}
		}
		return mc;
	}
	
	public static void main(String[] args) {
		Amostra a = new Amostra("bcancer.csv");
		Grafosorientados g = new Grafosorientados(a.dim()-1);
		BN2 b = new BN2(a,g,0.5);
		System.out.print(b);
		int m1[]= {0,0,0,1,0,0,0,0,0,0,0};
		int m2[]= {0,0,0,1,0,0,0,0,0,0,1};
		System.out.println(b.prob(m1));
		System.out.println(b.prob(m2));
		System.out.println(b.prob(m1)/(b.prob(m1)+b.prob(m2)));
		System.out.println(b.prob(m2)/(b.prob(m1)+b.prob(m2)));
		int m[]= {0,0,0,1,0,0,0,0,0,0};
		System.out.println(b.classify(m));
	}
}
