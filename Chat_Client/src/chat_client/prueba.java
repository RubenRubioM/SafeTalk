/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat_client;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Ruben
 */
public class prueba {
    
    public static void main(String args[]) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        //Generamos las claves públicas y privadas
        String mensaje = "Hola helena";
        
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair kp = kpg.generateKeyPair();
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();
        String base64PB = clavePublicaToBase64(publicKey);
        PublicKey pb = Base64ToClavePublica(base64PB);
        System.out.println(mensaje);
        
        Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, pb);
        byte[] mensajeCifrado = rsa.doFinal(mensaje.getBytes());
        
        String mensajeCifrado2 = new String(mensajeCifrado);
        System.out.println(mensajeCifrado2);
        
        rsa.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] mensajeDescifrado = rsa.doFinal(mensajeCifrado);
        String mensajeDescifrado2 = new String(mensajeDescifrado);
        
        System.out.println(mensajeDescifrado2);

    }
    
    
    private static String clavePublicaToBase64(PublicKey pb){
        byte[] clave= pb.getEncoded();
        String RSAPublicaBase64 = Base64.getEncoder().encodeToString(clave); 
        
        return RSAPublicaBase64;
    }
    
    private static PublicKey Base64ToClavePublica(String claveBase64) throws NoSuchAlgorithmException{
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
}
