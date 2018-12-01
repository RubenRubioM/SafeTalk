package chat_server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import javax.crypto.Cipher;

class Usuario{
    String nombre;
    String clave;
    
    public Usuario(String nombre, String clave){
        this.nombre=nombre;
        this.clave=clave;
    }
    
    public String getNombre(){
        return nombre;
    }
    public String getClave(){
        return clave;
    }
    
    
}

public class server_frame extends javax.swing.JFrame 
{
   
   ArrayList clientOutputStreams;
   ArrayList<Usuario> usuarios;

   public class ClientHandler implements Runnable	
   {
       BufferedReader reader;
       Socket sock;
       PrintWriter client;

       public ClientHandler(Socket clientSocket, PrintWriter user) 
       {
            client = user;
            try 
            {
                sock = clientSocket;
                InputStreamReader isReader = new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(isReader);
            }
            catch (Exception ex) 
            {
                ta_chat.append("Unexpected error... \n");
            }

       }

       @Override
       public void run() 
       {
            String message, connect = "Connect", disconnect = "Disconnect", chat = "Chat" ;
            String[] data;
            
            try 
            {
                while ((message = reader.readLine()) != null) 
                {
                    //message tiene la linea que enviamos por socket
                    ta_chat.append("Recivido: " + message + "\n");
                    data = message.split(":");
                    System.out.println(message);
                    
                    if(data.length>=3){
                        if (data[2].equals(connect)) 
                        {
                            //Usuario:se ha conectado:Connect:ClavePublicaRSA

                            //Comprobamos si es el primer usuario en conectares
                            System.out.println("Entra al conectar usuario");
                            tellEveryone((data[0] + ": entró al chat. :" + chat));
                            userAdd(data[0],data[3]);


                        }
                        else if (data[2].equals(disconnect)) 
                        {
                            //Usuario:se ha desconectado:Disconnect
                            tellEveryone((data[0] + ":se ha desconectado. :" + chat+":"+"Conexion"));
                            userRemove(data[0]);
                        } 
                        else if (data[2].equals(chat)) 
                        {
                            if(data[3].equals("MensajeEncriptado")){
                                //Usuario1:njauhsyfduhasdf:Chat:MensajeEncriptado
                                tellEveryone(data[0]+":"+data[1]+":"+"Chat"+":"+"MensajeDesencriptado");
                            }else{
                                //Usuario:mensaje:Chat
                                 tellEveryone(message);
                            }
                            
                        }else if(data[2].equals("ClaveAESEncriptada")){
                            
                            //Usuario que gestiona la clave:ClaveAESCifrada:ClaveAESEncriptada:Usuario2
                            //Recibimos la clave AES encriptada y la enviamos al usuario que la solicito
                            System.out.println("Recibimos la clave AES encriptada y la enviamos al usuario que la solicitó");
                            
                            tellEveryone(data[3]+":"+data[1]+":"+"ClaveAESDesencriptada");
                            //Enviamos:       Usuario2:ClaveAES:ClaveAESDesencriptada
                        }
                    }else if(data.length>=2){
                        if(data[1].equals("PedirAES")){
                        
                            if(usuarios.size()==1){
                                //Creara la clave AES si no hay usuarios en la lista
                                System.out.println("Es el primero y le enviamos un mensaje para que cree la clave AES");
                                tellEveryone(data[0]+":"+"CrearAES");
                            }else{
                                //Pedira la clave AES
                                System.out.println("Ya hay una clave AES creada, envia un mensaje para soliticarla");
                                Iterator it = usuarios.iterator();
                                boolean check = false;

                                //Buscamos la clave del usuario(data[0])
                                String usuDestino = usuarios.get(0).getNombre();
                                while(it.hasNext()){
                                    Usuario usu = (Usuario)it.next();
                                    if(usu.getNombre().equals(data[0]) && check==false){
                                        check=true;
                                        //Usuario que pide la clave:PeticionAES:ClavePublicaAES
                                        tellEveryone(data[0]+":PeticionAES:"+usu.getClave()+":"+usuDestino);
                                    }
                                } 
                            }
                        }
                    }
                     
                    else 
                    {
                        ta_chat.append("No Conditions were met. \n");
                    }
                } 
             } 
             catch (Exception ex) 
             {
                ta_chat.append("Conexión perdida. \n");
                ex.printStackTrace();
                clientOutputStreams.remove(client);
             } 
	} 
    }

    public server_frame() 
    {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        ta_chat = new javax.swing.JTextArea();
        b_start = new javax.swing.JButton();
        b_end = new javax.swing.JButton();
        b_users = new javax.swing.JButton();
        b_clear = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Chat - Server's frame");
        setName("server"); // NOI18N
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        ta_chat.setColumns(20);
        ta_chat.setRows(5);
        jScrollPane1.setViewportView(ta_chat);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(39, 101, 430, 278));

        b_start.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        b_start.setText("Inicio");
        b_start.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_startActionPerformed(evt);
            }
        });
        getContentPane().add(b_start, new org.netbeans.lib.awtextra.AbsoluteConstraints(62, 11, 176, -1));

        b_end.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        b_end.setText("Cerrar");
        b_end.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_endActionPerformed(evt);
            }
        });
        getContentPane().add(b_end, new org.netbeans.lib.awtextra.AbsoluteConstraints(62, 52, 176, -1));

        b_users.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        b_users.setText("Usuarios Conectados");
        b_users.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_usersActionPerformed(evt);
            }
        });
        getContentPane().add(b_users, new org.netbeans.lib.awtextra.AbsoluteConstraints(248, 11, 192, -1));

        b_clear.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        b_clear.setText("Limpiar");
        b_clear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_clearActionPerformed(evt);
            }
        });
        getContentPane().add(b_clear, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 400, 77, -1));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/fondo_degradado_walimex_1_5x2m_azul.jpg"))); // NOI18N
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 480, 460));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void b_endActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_endActionPerformed
        try 
        {
            Thread.sleep(5000);                 //5000 milliseconds is five second.
        } 
        catch(InterruptedException ex) {Thread.currentThread().interrupt();}
        
        tellEveryone("Server:is stopping and all users will be disconnected.\n:Chat");
        ta_chat.append("Parando servidor... \n");
        
        ta_chat.setText("");
        
    }//GEN-LAST:event_b_endActionPerformed

    private void b_startActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_startActionPerformed
        Thread starter = new Thread(new ServerStart());
        starter.start();
       
        
        ta_chat.append("Server started...\n");
    }//GEN-LAST:event_b_startActionPerformed

    private void b_usersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_usersActionPerformed
        ta_chat.append("\n Usuarios conectados : \n");
        for (Usuario current_user : usuarios)
        {
            ta_chat.append(current_user.getNombre());
            ta_chat.append("\n");
        }    
        
    }//GEN-LAST:event_b_usersActionPerformed

    private void b_clearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_clearActionPerformed
        ta_chat.setText("");
    }//GEN-LAST:event_b_clearActionPerformed

    public static void main(String args[]) 
    {
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            @Override
            public void run() {
                new server_frame().setVisible(true);
            }
        });
    }
    
    public class ServerStart implements Runnable 
    {
        @Override
        public void run() 
        {
            clientOutputStreams = new ArrayList();
            usuarios = new ArrayList();  

            try 
            {
                ServerSocket serverSock = new ServerSocket(2222);

                while (true) 
                {
                    Socket clientSock = serverSock.accept();
                    PrintWriter writer = new PrintWriter(clientSock.getOutputStream());
                    clientOutputStreams.add(writer);

                    Thread listener = new Thread(new ClientHandler(clientSock, writer));
                    listener.start();
                    ta_chat.append("Tenemos una conexión. \n");
                }
            }
            catch (Exception ex)
            {
                ta_chat.append("Error al hacer la conexión. \n");
            }
        }
    }
    
    public void userAdd (String name,String clave) 
    {
        String message, add = ": :Connect", done = "Server: :Done";
        
        Usuario usu = new Usuario(name,clave);
        usuarios.add(usu);

        for (Usuario token:usuarios) 
        {
            message = (token.getNombre() + add);
            tellEveryone(message);
        }
        tellEveryone(done);
        
        
    }
    
    public void userRemove (String name) 
    {
        String message, add = ": :Connect", done = "Server: :Done";
        Iterator it = usuarios.iterator();
        
        while(it.hasNext()){
            Usuario usu = (Usuario)it.next();
            if(usu.getNombre().equals(name)){
                it.remove();
                message = (usu.getNombre() + add);
                tellEveryone(message);
            }
        }

        
        tellEveryone(done);
    }
    

    
    public void tellEveryone(String message) 
    {
	Iterator it = clientOutputStreams.iterator();

        while (it.hasNext()) 
        {
            try 
            {
                PrintWriter writer = (PrintWriter) it.next();
		writer.println(message);
		ta_chat.append("Enviando: " + message + "\n");
                writer.flush();
                ta_chat.setCaretPosition(ta_chat.getDocument().getLength());

            } 
            catch (Exception ex) 
            {
		ta_chat.append("Error telling everyone. \n");
            }
        } 
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton b_clear;
    private javax.swing.JButton b_end;
    private javax.swing.JButton b_start;
    private javax.swing.JButton b_users;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea ta_chat;
    // End of variables declaration//GEN-END:variables
}
