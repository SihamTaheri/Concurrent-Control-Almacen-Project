package cc.controlAlmacen;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

// paso de mensajes con JCSP
import org.jcsp.lang.*;




// 
// TODO: otros imports
// 
// 

public class ControlAlmacenCSP implements ControlAlmacen, CSProcess {
    // algunas constantes de utilidad
    static final int PRE_KO  = -1;
    static final int NOSTOCK =  0;
    static final int STOCKOK =  1;
    static final int SUCCESS =  0;
    static final int NOOK	 =	2;
    
    // TODO: añadid las que creáis convenientes
    
    // canales para comunicación con el servidor
    private final Any2OneChannel chComprar            = Channel.any2one();
    private final Any2OneChannel chEntregar           = Channel.any2one();
    private final Any2OneChannel chDevolver           = Channel.any2one();
    private final Any2OneChannel chOfrecerReabastecer = Channel.any2one();
    private final Any2OneChannel chReabastecer        = Channel.any2one();

    // Resource state --> server side !!

    // peticiones de comprar
    private static class PetComprar {
	public String productId;
	public int    q;
	public One2OneChannel chresp;

	PetComprar (String productId, int q, One2OneChannel chresp) {
	    this.productId = productId;
	    this.q         = q;
	    this.chresp = chresp;
	}
    }

    // peticiones de entregar
    private static class PetEntregar {
	     public String productId;
    	public int    q;
    	public One2OneChannel chentreg;
	//
    PetEntregar(String productId, int q) {
    	   this.productId = productId;
    	   this.q         = q;
    	   this.chentreg = Channel.one2one();
    	}
    }

    // peticiones de devolver
    private static class PetDevolver {
    	public String productId;
    	public int    q;
    	public One2OneChannel chresp;

    	PetDevolver (String productId, int q) {
    	    this.productId = productId;
    	    this.q         = q;
    	    this.chresp = Channel.one2one();
    	}
    }

    // para aplazar peticiones de ofrecerReabastecer
    private static class PetOfrecerReabastecer {
    	public String productId;
    	public int    q;
    	public One2OneChannel chresp;

    PetOfrecerReabastecer (String productId, int q) {
    	    this.productId = productId;
    	    this.q         = q;
    	    this.chresp = Channel.one2one();
    	} 
    }

    // peticiones de reabastecer
    private static class PetReabastecer {
    	public String productId;
    	public int    q;
    	public One2OneChannel chresp;

    	PetReabastecer (String productId, int q) {
    	    this.productId = productId;
    	    this.q         = q;
    	    this.chresp = Channel.one2one();
    	}
    }
    
    // INTERFAZ ALMACEN
    public boolean comprar(String clientId, String itemId, int cantidad) {

        
	    System.out.println("compra");
     // petición al servidor
	    
	PetComprar pet = new PetComprar(itemId,cantidad, Channel.one2one());
	
	chComprar.out().write(pet);
	
	 System.out.println("compra2");
    // recibimos contestación del servidor
	// puede ser una de {PRE_KO, NOSTOCK, STOCKOK}
	int respuesta = (Integer) pet.chresp.in().read();

	// no se cumple PRE:
	if (respuesta == PRE_KO) {
	    throw new IllegalArgumentException();
    }

	return(respuesta == STOCKOK);
	
	}
    

    public void entregar(String clientId, String itemId, int cantidad) { //9
	
    	
    
  	  
    PetEntregar pet = new PetEntregar(itemId,cantidad);
    chEntregar.out().write(pet);
    
     
     int respuesta = (Integer)pet.chentreg.in().read();
     
     // no se cumple PRE:
 	if (respuesta == PRE_KO)
 	    throw new IllegalArgumentException();
 	
 	if(respuesta == SUCCESS);
 	  
 	/*
 	if(respuesta == NOSTOCK)
 		chEntregar.out().write(pet);
 	// se cumple PRE:
 	 * 
 	 */
 		
    }

    public void devolver(String clientId, String itemId, int cantidad) {
    	// petición al servidor
    	PetDevolver pet = new PetDevolver(itemId,cantidad);
    	chDevolver.out().write(pet);
    	
        // recibimos contestación del servidor
    	// puede ser una de {PRE_KO, NOSTOCK, STOCKOK}
    	int respuesta = (Integer) pet.chresp.in().read();

    	// no se cumple PRE:
    	if (respuesta == PRE_KO)
    	    throw new IllegalArgumentException();
    	// se cumple PRE:
    	
    	
    }

    public void ofrecerReabastecer(String itemId, int cantidad) {
    	

        PetOfrecerReabastecer pet = new PetOfrecerReabastecer(itemId,cantidad);
        chOfrecerReabastecer.out().write(pet);
        
         
         int respuesta = (Integer)pet.chresp.in().read();
         
         // no se cumple PRE:
     	if (respuesta == PRE_KO)
     	    throw new IllegalArgumentException();
     	
     	if (respuesta == SUCCESS);
    	
     	/*
     	if(respuesta == NOSTOCK)
     		chOfrecerReabastecer.out().write(pet);
     	// se cumple PRE:
     	 * 
     	 */
    }
    public void reabastecer(String itemId, int cantidad) {
    	// petición al servidor
    	PetReabastecer pet = new PetReabastecer(itemId,cantidad);
    	chReabastecer.out().write(pet);
    	
        // recibimos contestación del servidor
    	// puede ser una de {PRE_KO, NOSTOCK, STOCKOK}
    	int respuesta = (Integer)pet.chresp.in().read();

    	// no se cumple PRE:
    	if (respuesta == PRE_KO)
    	    throw new IllegalArgumentException();
    	
    	
    	
		  //se tiene que ir a los codigos de desbloqueo
    	/*
    	if(respuesta == NOOK)
    		 chReabastecer.out().write(pet);
    	// se cumple PRE:
    	 * 
    	 */
    }
	
    // atributos de la clase
    Map<String,Integer> tipoProductos; // stock mínimo para cada producto
	private Map<String,Integer>  enCamino;
	private Map<String,Integer>  comprados;
	private Map<String,Integer>  disponibles;
	
    public ControlAlmacenCSP(Map<String,Integer> tipoProductos) {

	new ProcessManager(this).start(); // al crearse el servidor también se arranca...
	this.tipoProductos=new HashMap<String,Integer>(tipoProductos);
	this.comprados=new HashMap<String,Integer>();
	this.disponibles=new HashMap<String,Integer>();
	this.enCamino=new HashMap<String,Integer>();
	
	System.out.println("NUEVA PRUEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"+ tipoProductos.toString());
    }
    
 
// SERVIDOR
    public void run() {
	// para recepción alternativa condicional
	Guard[] entradas = {
	    chComprar.in(),
	    chEntregar.in(),
	    chDevolver.in(),
	    chOfrecerReabastecer.in(),
	    chReabastecer.in()
	};
	Alternative servicios =  new Alternative (entradas);
	// OJO ORDEN!!
	final int COMPRAR             = 0;
	final int ENTREGAR            = 1;
	final int DEVOLVER            = 2;
	final int OFRECER_REABASTECER = 3;
	final int REABASTECER         = 4;
	// condiciones de recepción: todas a CIERTO

	// estado del recurso
	final boolean[] sincCond = new boolean[5];
	// TODO: vuestra estructura de datos traída de monitores
	
	LinkedList <PetOfrecerReabastecer> bufO= new LinkedList<PetOfrecerReabastecer>();
	
	Map<String, Queue<PetEntregar>> bufE = new HashMap<>();
	// entregar
	// TODO: completar
	
	
	// ofrecerReabastecer
	// 
	// TODO: completar 
	// 

	// inicializaciones
	// 
	// 
	// TODO: completar
	// 
	// 
	// 
    
	// bucle de servicio
	while (true) {
	    // vars. auxiliares
	    // tipo de la última petición atendida
	    int choice = -1; // una de {COMPRAR, ENTREGAR, DEVOLVER, OFRECER_REABASTECER, REABASTECER}
	    
	    
	    // todas las peticiones incluyen un producto y una cantidad
	    //MyItem item  = new MyItem(99999);
	    String pid   = "";
	    int cantidad = -1;

	    choice = servicios.fairSelect();
	    
	    
	    //PetComprar petC;
	   
	    
	    
	    System.out.println("elije");
	    
		switch (choice) { 
		 
			case COMPRAR: // CPRE = Cierto
				 System.out.println("entra en el case");
				 System.out.println("OJOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO CHOICE DE COMPRAR ES ----------------------------------------------= "+ choice);
				 
		 PetComprar petC = (PetComprar) chComprar.in().read();	
		  // comprobar PRE:
		  // ** CÓDIGO ORIENTATIVO!! ADAPTAD A VUESTRA ESTRUCTURA DE DATOS!! **
		System.out.println("coso entra");
		
		pid = petC.productId;
		
		if(comprados.get(pid) == null) { //NUNCA SE HA HECHO UNA COMPRA DE ESTE PRODUCTO
	    	  comprados.put(pid, 0);
	    } 
	    if(enCamino.get(pid)== null) {
			enCamino.put(pid,0);
		}
	   
	    if(disponibles.get(pid) == null) { //NUNCA SE HA HECHO UNA COMPRA DE ESTE PRODUCTO
	    	  disponibles.put(pid, 0);
	    }
	    
		//item = itemsX.get(pid);
		
		cantidad = petC.q;
		
		if (cantidad < 1 || !tipoProductos.containsKey(pid) ) { // PRE_KO
		
			petC.chresp.out().write(PRE_KO);
		
		} else { // PRE_OK  
			
			boolean result = (disponibles.get(pid) + enCamino.get(pid) >= cantidad + comprados.get(pid));

			if (result) { // hay stock suficiente
				
				System.out.println("LA CANTIDAD DE COMPRADOS DE " + pid + " AUMENTA DE "+ comprados.get(pid) +" A "+ comprados.get(pid) + cantidad +";**********" );
				comprados.put(pid, comprados.get(pid) + cantidad);
				petC.chresp.out().write(STOCKOK);
				
		    } else { // no hay stock suficiente
		    	petC.chresp.out().write(NOSTOCK);
		    	System.out.println("ENTRA MAMAHUEVO");
		    }
		}
		break;	
	    case ENTREGAR: // CPRE en diferido
	    	
	    	PetEntregar petE = (PetEntregar)chEntregar.in().read();
			
			pid = petE.productId;

			 
		  	  if(disponibles.get(pid) == null) { //NUNCA SE HA HECHO UNA COMPRA DE ESTE PRODUCTO
		      	  disponibles.put(pid, 0);
		        }
			
			cantidad = petE.q;
			
			System.out.println("ENTRA EN ENREGUUUUUUUUUUUUUR de " + petE.productId);
			System.out.println("DISPONIBLES "+disponibles.get(petE.productId)+"; ENCAMINO "+enCamino.get(petE.productId)+" ; COMPRADOS" + comprados.get(petE.productId));
			
			if (cantidad < 1 || !tipoProductos.containsKey(pid)) { // PRE_KO
			
				System.out.println("SA METIO EN LA EXCEPTION EL MAMARACHOOOOOOO");
				petE.chentreg.out().write(PRE_KO);
		
			} else {
				// PRE_OK 
				if(disponibles.get(petE.productId)>=petE.q) {// CPRE OK => hacemos la op
					disponibles.put(petE.productId, disponibles.get(petE.productId)- cantidad);
					comprados.put(petE.productId, comprados.get(petE.productId)-cantidad);
					petE.chentreg.out().write(SUCCESS);
	    		 
				}else {// CPRE KO => aplazar
					
					Queue<PetEntregar> values = bufE.getOrDefault(pid, new LinkedList<>());
			        
					values.add(petE);
			        bufE.put(pid, values);

				}		
			    
				}			
		break;
	    case DEVOLVER: // CPRE = Cierto
	    	
	    	PetDevolver petD = (PetDevolver)chDevolver.in().read();	
			  // comprobar PRE:
			  // ** CÓDIGO ORIENTATIVO!! ADAPTAD A VUESTRA ESTRUCTURA DE DATOS!! **
			
			pid = petD.productId;

	    	if(enCamino.get(pid)==null) {
				enCamino.put(pid,0);
			}
			
			if(comprados.get(pid) == null) { 
		    	  comprados.put(pid, 0);
		    }
			if(disponibles.get(pid) == null) { 
		    	  disponibles.put(pid, 0);
		    }
			
			cantidad = petD.q;
			
			if (cantidad < 1 || !tipoProductos.containsKey(pid)) { // PRE_KO
			
				petD.chresp.out().write(PRE_KO);
			
			} else { // PRE_OK  
				disponibles.put(pid, disponibles.get(pid)+ cantidad); 
				petD.chresp.out().write(SUCCESS);
				

			    } 
			
		break;
	    case OFRECER_REABASTECER: // CPRE en diferido
	    	
			
			System.out.println("ENTRA en ofrecer");
			
	     PetOfrecerReabastecer petO = (PetOfrecerReabastecer)chOfrecerReabastecer.in().read();
			
	     System.out.println("RECIBE LA PETICION ORFRECER");
	     
			pid = petO.productId;
			
			if(enCamino.get(pid)==null) {
				enCamino.put(pid,0);
			}
			
			if(comprados.get(pid) == null) { 
		    	  comprados.put(pid, 0);
		    }
			if(disponibles.get(pid) == null) { 
		    	  disponibles.put(pid, 0);
		    }
			
			
			cantidad = petO.q;
			
			if (cantidad < 1 || !tipoProductos.containsKey(pid)) { // PRE_KO	
				petO.chresp.out().write(PRE_KO);
			} else { // PRE_OK 
				System.out.println("ENTRA EN OFRECERREABASTECEEEEEEEEEEEEEEER de " + petO.productId);
				System.out.println("DISPONIBLES "+disponibles.get(petO.productId)+"; ENCAMINO "+enCamino.get(petO.productId)+" ; COMPRADOS" + comprados.get(petO.productId));
				if((disponibles.get(petO.productId)+enCamino.get(petO.productId) - comprados.get(petO.productId) < tipoProductos.get(petO.productId))){
		    		
					enCamino.put(petO.productId, enCamino.get(petO.productId)+cantidad);
					petO.chresp.out().write(SUCCESS);
	
		    		
				}else {
					System.out.println("SE METE EN LA COLA DE OFRECER ");
						bufO.add(petO);
			    
				}
			    
			}
		break;
	    case REABASTECER: // CPRE = Cierto
	    	PetReabastecer petR = (PetReabastecer) chReabastecer.in().read();
			
			pid = petR.productId;
		
			int cantidadR = petR.q;
			
			if (cantidadR < 1 || !tipoProductos.containsKey(pid)) { // PRE_KO
			
				petR.chresp.out().write(PRE_KO);
		
			} else { // PRE_OK 
				
				System.out.println("ENTRA EN REABASTECEEEEEEEEEEEERASECAAAAAS");
				
				System.out.println("EL VALOR ACTUAL DE ENCAMINO ES = "+  enCamino.get(pid) + "; EL DE CANTIDAD ES "+ cantidadR);
				 enCamino.put(pid, enCamino.get(pid)-cantidadR);
				 disponibles.put(pid, disponibles.get(pid)+cantidadR);
				 
			     petR.chresp.out().write(SUCCESS);  
			    } 
			
		break;
	    } // switch
		
		
	 System.out.println("SE SALEEEEEEEEEEEEE--------------------------------------------------");
	///////////////////PETICIONES DESBLOQUEO 
	
	    // tratamiento de peticiones aplazadas
	 String[] keys = bufE.keySet().toArray(new String[0]);

	 
     // Recorrer el buffer utilizando las claves
     for (String key : keys) {
    	 
         Queue<PetEntregar> petQueue = bufE.get(key);
         
         boolean fallo=false;
         int m=0;
         int tamano3=petQueue.size();
         
         while (!petQueue.isEmpty() && !fallo) {
        	 
           PetEntregar pet = petQueue.peek();
           
           System.out.println("nombre"+ pet.productId + "cantidad"+ pet.q + "OJOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
           
	    	int cantidadE=pet.q;

	    	boolean resu=disponibles.get(pet.productId)>=cantidadE;
	    	
	    	System.out.println("disponibles"+ disponibles.get(pet.productId) + "cantidad que pido"+ pet.q + "mmmmm");
	    	
	    	   if(resu) {
	    		   
	    		   
	    		System.out.println("nombre"+ pet.productId + "cantidad"+ pet.q + "OSESALIOOOOOOOOOOOOOOOOOOSESALIOSEJSJBFJBHDJSFHBKJNSDFKZJSN");
	    		
	    		disponibles.put(pet.productId, disponibles.get(pet.productId) - cantidadE);
				comprados.put(pet.productId, comprados.get(pet.productId)-cantidadE);	
				petQueue.poll();
				pet.chentreg.out().write(SUCCESS);
			   
	    	  }else {
	    		  
	    		  System.out.println("nombre"+ pet.productId + "cantidad"+ pet.q + "TODAVIAAAAAAAAAAAAAAAAAAAAAAAA NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO MARRANOOOO");
	    		  fallo=true;
	    	  }
	    	m++;   //1 MIN EN EL ALMACEN,  Y TENEMOS UNA COMPRA , DISPONIBLES=5
	     }
     }
         
	     
	 
	     int nO= bufO.size();
	     
	     System.out.println("LA CANTIDAD QUE HAY EN ESPERA SON " + nO);
	     for(int i=0;i<nO ;i++) {
	    	 
	    	 PetOfrecerReabastecer petO = bufO.get(i); 
	    	 
	    	 
	    	 int cantidubi= petO.q;
	    	// if(choice==0 && petO.productId.equals(pid)) {
	 
	    	 System.out.println("EHHHH LA EVALUASION EVALUADA DE PETOFRECERREABASTECER OJOOJITO de "+  petO.productId);
				
	    	 System.out.println("REVISION PET: DISPONIBLES "+disponibles.get(petO.productId)+"; ENCAMINO "+enCamino.get(petO.productId)+" ; COMPRADOS" + comprados.get(petO.productId));
	    	 
	    	if((disponibles.get(petO.productId)+enCamino.get(petO.productId) - comprados.get(petO.productId) < tipoProductos.get(petO.productId))){
	    		enCamino.put(petO.productId, enCamino.get(petO.productId)+cantidubi);
	    		bufO.remove(i);
	    		petO.chresp.out().write(SUCCESS);
	    		
	    		i++;
	    		
	    	}}
	    	

    	 System.out.println("SASALIO BIEN");
	 		    
	}
	     
	     
	     
	     
	} // bucle servicio
    } // run SERVER

    // Clases auxiliares
    //
    // TODO: traed de vuestra sol. por monitores
    //
    

