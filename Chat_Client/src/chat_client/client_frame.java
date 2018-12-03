package chat_client;


import java.net.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;



public class client_frame extends javax.swing.JFrame 
{
    String username, address = "localhost";
    PrivateKey miClavePrivadaRSA;
    PublicKey miClavePublicaRSA;
    
    Key KeyAES;
    private static Key KeyAESfija;
    
    ArrayList<String> users = new ArrayList();
    int port = 2222;
    Boolean isConnected = false;
    
    Socket sock;
    BufferedReader reader;
    PrintWriter writer;
    
    
    //--------------------------//
    
    public void ListenThread() 
    {
         Thread IncomingReader = new Thread(new IncomingReader());
         IncomingReader.start();
    }
    
    //--------------------------//
    
    public void userAdd(String data) 
    {
         users.add(data);
    }
    
    //--------------------------//
    
    public void userRemove(String data) 
    {
         ta_chat.append(data + " is now offline.\n");
    }
    
    //--------------------------//
    
    public void writeUsers() 
    {
         String[] tempList = new String[(users.size())];
         users.toArray(tempList);
         for (String token:tempList) 
         {
             //users.append(token + "\n");
         }
    }
    
    //--------------------------//
    
    public void sendDisconnect() 
    {
        String bye = (username + ": :Disconnect");
        try
        {
            writer.println(bye); 
            writer.flush(); 
        } catch (Exception e) 
        {
            ta_chat.append("Could not send Disconnect message.\n");
        }
    }

    //--------------------------//
    
    public void Disconnect() 
    {
        try 
        {
            ta_chat.append("Desconectado.\n");
            sock.close();
        } catch(Exception ex) {
            ta_chat.append("Fallo al desconectarse. \n");
        }
        isConnected = false;
        tf_username.setEditable(true);

    }
    
    public client_frame() 
    {
        initComponents();
    }
    
    //--------------------------//
    
    public class IncomingReader implements Runnable
    {
        @Override
        public void run() 
        {
            String[] data;
            String stream, done = "Done", connect = "Connect", disconnect = "Disconnect", chat = "Chat";

            try 
            {
                while ((stream = reader.readLine()) != null) 
                {
                     data = stream.split(":");
                     //System.out.println(stream);
                    //Tenemos que hacer una comprobacion de cuantos campos nos ha enviado el usuario para que al hacer data[2] no nos de un indexoutofbounds
                    switch (data.length) {
                        case 2:
                            if(data[1].equals("CrearAES")){
                                
                                //CrearAES
                                //El primer usuario crea la clave AES y no hace nada mas
                                System.out.println("El primer usuario ha creado la clave AES");
                                crearAES();
                                
                            }   break;
                        case 3:
                            if(data[2].equals(connect)){
                                //Usuario: :Connect
                                ta_chat.removeAll();
                                userAdd(data[0]);
                            }else if(data[2].equals(disconnect)){
                                userRemove(data[0]);
                            }else if(data[2].equals(done)){
                                writeUsers();
                                users.clear();
                                
                                //Nos llega la clave AES encriptada y la tenemos que desencriptar
                            }else if(data[2].equals("ClaveAESDesencriptada") && data[0].equals(username)){
                                //Usuario2:ClaveAESBase64:ClaveAESDesencriptada
                                //En el mensaje nos llegará la clave AES cifrada con nuestra clave publica y debemos desencriptarla
                                
                                try {
                                    byte[] data2 = Base64.getDecoder().decode(data[1]);
                                    Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                                    rsa.init(Cipher.DECRYPT_MODE, miClavePrivadaRSA);
                                    byte[] aesDesencriptada=rsa.doFinal(data2);
                                    KeyAES= Base64ToKey(aesDesencriptada);
                                    System.out.println("la calve aes es: " + KeyAES);
                                    System.out.println("El usuario recibe la clave AES y la desencripta con su clave privada");
                                    
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                                
                                
                            }   break;
                        case 4:
                            if (data[2].equals(chat))
                            {
                                //Te llega un mensaje encriptado
                                if(data[3].equals("MensajeDesencriptado")){
                                    
                                     //////////////////////////////
                                    //  DESENCRIPTAMOS MENSAJE  //
                                    /////////////////////////////
                                    //Usuario1:hola:Chat:MensajeDesencriptado
                                    Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
                                    System.out.println("Mensaje recivido");
                                    byte[] mensaje = Base64.getDecoder().decode(data[1].getBytes());
                                    aes.init(Cipher.DECRYPT_MODE, KeyAESfija);
                                    byte[] encriptado = aes.doFinal(mensaje);
                                    String mensajeDesencriptado = new String(encriptado);
                                    
                                    ta_chat.append(data[0] + ":" + mensajeDesencriptado + "\n");
                                    ta_chat.setCaretPosition(ta_chat.getDocument().getLength());
                                    
                                }else{
                                    ta_chat.append(data[0] + ":" + data[1] + "\n");
                                    ta_chat.setCaretPosition(ta_chat.getDocument().getLength());
                                }
                                
                                //Te llega una peticion para que envies la clave AES a un usuario en concreto
                            }else if(data[1].equals("PeticionAES") && data[3].equals(username)){
                                //Usuario que la pide:PeticionAES:ClavePublicaBase64:Usuario que gestiona las claves
                                //Recoge la clavePublica del destinatario y ahora debe enviarle la clave AES encriptada con esa clave publica
                                
                                PublicKey pkUsuario = Base64ToClavePublica(data[2]);
                                //El envio sera tal que:   Usuario que gestiona las claves:ClaveAES:ClaveAESEncriptada:Usuario que pedia la clave
                                
                                byte[] claveAESCifrada = null;
                                String claveAESBase64 = null;
                                
                                String claveAESBase64Cifrada = null;
                                
                                
                                //TODO Segun entiendo se inicializa mal el encriptador y por eso falla al hacer el doFinal (acordarte de cambiar miClavePublicaRSA por pkUsuario
                                try {
                                    Cipher rsa = Cipher.getInstance("RSA");
                                    claveAESBase64 = KeyToBase64(KeyAES);
                                    
                                    rsa.init(Cipher.ENCRYPT_MODE, pkUsuario);
                                    
                                    claveAESCifrada = rsa.doFinal(claveAESBase64.getBytes());
                                    claveAESBase64Cifrada = ByteToBase64(claveAESCifrada);
                                    
                                    System.out.println("El usuario envia la clave AES encriptada con la clave publica del otro usuario");
                                } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException ex) {
                                    ex.printStackTrace();
                                }
                                
                                try {
                                    writer.println(username+":"+claveAESBase64Cifrada+":"+"ClaveAESEncriptada"+":"+data[0]);
                                    writer.flush();
                                } catch (Exception e) {
                                    
                                }
                                
                                
                            }   break;
                        default:
                            break;
                    }
                    
                     
                }
           }catch(Exception ex) {
               
               ex.printStackTrace();
           }
        }
    }

    //--------------------------//
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        b_disconnect = new javax.swing.JButton();
        lb_address = new javax.swing.JLabel();
        tf_address = new javax.swing.JTextField();
        lb_username = new javax.swing.JLabel();
        tf_username = new javax.swing.JTextField();
        lb_password = new javax.swing.JLabel();
        tf_password = new javax.swing.JTextField();
        b_connect = new javax.swing.JButton();
        b_anonymous = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        ta_chat = new javax.swing.JTextArea();
        tf_chat = new javax.swing.JTextField();
        b_send = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Chat - Client's frame");
        setName("client"); // NOI18N
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        b_disconnect.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        b_disconnect.setText("Desconectar");
        b_disconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_disconnectActionPerformed(evt);
            }
        });
        getContentPane().add(b_disconnect, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 10, 230, 60));

        lb_address.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        lb_address.setText("Servidor:");
        getContentPane().add(lb_address, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 62, -1));

        tf_address.setText("localhost");
        tf_address.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tf_addressActionPerformed(evt);
            }
        });
        getContentPane().add(tf_address, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 50, 70, 20));

        lb_username.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lb_username.setText("Usuario:");
        getContentPane().add(lb_username, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 62, -1));

        tf_username.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tf_usernameActionPerformed(evt);
            }
        });
        getContentPane().add(tf_username, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 10, 70, 20));

        lb_password.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lb_password.setText("Contraseña:");
        getContentPane().add(lb_password, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 50, 70, -1));
        getContentPane().add(tf_password, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 50, 70, -1));

        b_connect.setText("Conectar");
        b_connect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_connectActionPerformed(evt);
            }
        });
        getContentPane().add(b_connect, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 50, 180, -1));

        b_anonymous.setText("Invitado");
        b_anonymous.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_anonymousActionPerformed(evt);
            }
        });
        getContentPane().add(b_anonymous, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 10, 180, -1));

        ta_chat.setColumns(20);
        ta_chat.setRows(5);
        jScrollPane1.setViewportView(ta_chat);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 86, 470, 296));
        getContentPane().add(tf_chat, new org.netbeans.lib.awtextra.AbsoluteConstraints(22, 400, 340, 31));

        b_send.setText("Enviar");
        b_send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                b_sendActionPerformed(evt);
            }
        });
        getContentPane().add(b_send, new org.netbeans.lib.awtextra.AbsoluteConstraints(368, 400, 127, 31));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/fondo_degradado_walimex_1_5x2m_azul.jpg"))); // NOI18N
        jLabel1.setText("jLabel1");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 530, 460));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tf_usernameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tf_usernameActionPerformed
    
    }//GEN-LAST:event_tf_usernameActionPerformed

    ////////////////////
    //Conectar usuario//
    ////////////////////
    private void b_connectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_connectActionPerformed
                                
        
        if (isConnected == false) 
        {
            username = tf_username.getText();
         
            if (!username.equals("")){
                
                try {
                    //Creamos la clave privada y publica RSA
                    crearParDeClavesRSA();
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(client_frame.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                
                //Pasamos la clave publica a Base64
                String clavePublicaBase64 = clavePublicaToBase64();
                
                
                                    
                try {
                
                    sock = new Socket(address, port);
                    InputStreamReader streamreader = new InputStreamReader(sock.getInputStream());
                    reader = new BufferedReader(streamreader);
                    writer = new PrintWriter(sock.getOutputStream());
                    writer.println(username +":"+"conectado" +":Connect:"+clavePublicaBase64);
                    writer.flush();
                    writer.println(username +":"+"PedirAES");

                    writer.flush(); 
                    isConnected = true; 
                    b_disconnect.setVisible(true);
                    b_connect.setVisible(false);
                    b_anonymous.setVisible(false);
                    tf_password.setVisible(false);
                    lb_password.setVisible(false);
                    tf_username.setEditable(false);
                    tf_address.setEditable(false);

                } 
                catch (Exception ex) {
                
                    ta_chat.append("No te has podido conectar. \n");
                    tf_username.setEditable(true);
                }

                ListenThread();
            }else{
                ta_chat.append("No has puesto usuario. \n");
            }
        } else if (isConnected == true) 
        {
            ta_chat.append("Ya estas conectado. \n");
        }
    }//GEN-LAST:event_b_connectActionPerformed

    
    
    /////////////////
    //Desconectarse//
    /////////////////
    private void b_disconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_disconnectActionPerformed
        
        b_disconnect.setVisible(false);
        b_connect.setVisible(true);
        b_anonymous.setVisible(true);
        tf_password.setVisible(true);
        lb_password.setVisible(true);
        tf_username.setEditable(true);
        tf_address.setEditable(true);
        
        sendDisconnect();
        Disconnect();
    }//GEN-LAST:event_b_disconnectActionPerformed

    ////////////////////
    //Conectar anonimo//
    ////////////////////
    
    private void b_anonymousActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_anonymousActionPerformed
        tf_username.setText("");
        if (isConnected == false) 
        {
            String anon="anon";
            Random generator = new Random(); 
            int i = generator.nextInt(999) + 1;
            String is=String.valueOf(i);
            anon=anon.concat(is);
            username=anon;
            
            tf_username.setText(anon);
            tf_username.setEditable(false);

            try 
            {
                sock = new Socket(address, port);
                InputStreamReader streamreader = new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(streamreader);
                writer = new PrintWriter(sock.getOutputStream());
                writer.println(anon +":"+ miClavePublicaRSA+":Connect");
                writer.flush();                 
                isConnected = true; 
                b_disconnect.setVisible(true);
                b_connect.setVisible(false);
                b_anonymous.setVisible(false);
                tf_password.setVisible(false);
                lb_password.setVisible(false);
                tf_username.setEditable(false);
                tf_address.setEditable(false);
            } 
            catch (Exception ex) 
            {
                ta_chat.append("Cannot Connect! Try Again. \n");
                tf_username.setEditable(true);
            }
            
            ListenThread();
            
        } else if (isConnected == true) 
        {
            ta_chat.append("You are already connected. \n");
        }        
    }//GEN-LAST:event_b_anonymousActionPerformed

    
    //////////////////
    //Enviar mensaje//
    //////////////////
    private void b_sendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_b_sendActionPerformed
        String nothing = "";
        if ((tf_chat.getText()).equals(nothing)) {
            tf_chat.setText("");
            tf_chat.requestFocus();
        } else {
            try {
                ///////////////////////////////
                //  ENCRIPTAMOS MENSAJE     //
                /////////////////////////////
                
                
               String mensaje = tf_chat.getText();
               Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");              
               aes.init(Cipher.ENCRYPT_MODE, KeyAESfija);
               byte[] encriptado = aes.doFinal(mensaje.getBytes());
               //String mensajeEncriptado = new String(encriptado);
               //ta_chat.append(username+": "+mensaje);
               String mensaje2 = Base64.getEncoder().encodeToString(encriptado);
               writer.println(username + ":" + mensaje2 + ":" + "Chat"+":"+"MensajeEncriptado");
               writer.flush(); // flushes the buffer
               System.out.println("Mensaje enviado");

            } catch (Exception ex) {
                ta_chat.append("El mensaje no se ha podido enviar. \n");
                ex.printStackTrace();
            }
            tf_chat.setText("");
            tf_chat.requestFocus();
        }

        tf_chat.setText("");
        tf_chat.requestFocus();
    }//GEN-LAST:event_b_sendActionPerformed

    private void tf_addressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tf_addressActionPerformed

    }//GEN-LAST:event_tf_addressActionPerformed

    public static void main(String args[]) 
    {

        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {
                
                new client_frame().setVisible(true);
                                        b_disconnect.setVisible(false);

                
            }
        });
    }
    
    private void crearParDeClavesRSA() throws NoSuchAlgorithmException{
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(4096);
        KeyPair kp = keyPairGenerator.genKeyPair();
        miClavePublicaRSA = kp.getPublic();
        miClavePrivadaRSA  = kp.getPrivate();
    }
    
    private String clavePublicaToBase64(){
        byte[] clave= miClavePublicaRSA.getEncoded();
        String RSAPublicaBase64 = Base64.getEncoder().encodeToString(clave); 
        
        return RSAPublicaBase64;
    }
    
    private String KeyToBase64(Key clave){
        byte[] claveB = clave.getEncoded();
        String claveAESBase64 = Base64.getEncoder().encodeToString(claveB);
        
        return claveAESBase64;
    }
    
    private Key Base64ToKey(byte[] clave64) throws NoSuchAlgorithmException, InvalidKeyException{
        Key key = null;
        try {
            key = new SecretKeySpec(clave64, "AES");
        } catch (Exception e) {
        }
        
        return key;
    }
    
    private String ByteToBase64(byte[] clave){
        return Base64.getEncoder().encodeToString(clave);
      //  String devolucion = array.toString();
        //System.out.println("En la funcion"+clave+"\n"+devolucion);
        //return devolucion;
    }    
    private PublicKey Base64ToClavePublica(String claveBase64) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        byte[] decodi= Base64.getDecoder().decode(claveBase64);
        PublicKey pbdes = null;                  
                      
                             
        KeyFactory keyfactory = KeyFactory.getInstance("RSA");

        KeySpec keyspec = new X509EncodedKeySpec(decodi);
        try {
            pbdes = keyfactory.generatePublic(keyspec);
            
        } catch (InvalidKeySpecException ex) {
            Logger.getLogger(client_frame.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        return pbdes;
    }
    
    private void crearAES() throws NoSuchAlgorithmException{
        //Generamos una clave de 128 bits para AES
        
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        KeyAES = keyGenerator.generateKey();
                KeyAESfija = keyGenerator.generateKey();

        
        
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton b_anonymous;
    private javax.swing.JButton b_connect;
    private static javax.swing.JButton b_disconnect;
    private javax.swing.JButton b_send;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lb_address;
    private javax.swing.JLabel lb_password;
    private javax.swing.JLabel lb_username;
    private javax.swing.JTextArea ta_chat;
    private javax.swing.JTextField tf_address;
    private javax.swing.JTextField tf_chat;
    private javax.swing.JTextField tf_password;
    private javax.swing.JTextField tf_username;
    // End of variables declaration//GEN-END:variables

            
}
