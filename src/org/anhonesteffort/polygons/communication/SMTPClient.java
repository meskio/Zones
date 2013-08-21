package org.anhonesteffort.polygons.communication;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import android.util.Log;

// http://droidcoders.blogspot.com/2011/09/android-sending-emails-without-user.html
public class SMTPClient extends javax.mail.Authenticator {
  private static final String TAG = "org.anhonesteffort.polygons.communication.SMTPClient";
  public static enum AuthType {NONE, SSL, TLS};
  private String mUserName;
  private String mPassword;
  private String mHostName;
  private AuthType mAuthType;
  private int mPort;
  private MimeMessage emailMessage;

  public SMTPClient(String user, String password, String hostName, AuthType auth, int port) {
    mUserName = user;
    mPassword = password;
    mHostName = hostName;
    mAuthType = auth;
    mPort = port;
  }

  public void sendMessage(String recipient, String subject, String body)  throws AddressException, MessagingException {
    Log.d(TAG, "sendMessage(), sender: " + mUserName + ", recipient: " + recipient + ", subject: " + subject + ", body: " + body);
    Properties props;

    switch(mAuthType) {
      case NONE:
        props = getProperties();
        break;

      case SSL:
        props = getSSLProperties();
        break;

      case TLS:
        props = getTLSProperties();
        break;

      default:
        props = getProperties();
        break;
    }

    Session session = Session.getInstance(props, this);
    emailMessage = new MimeMessage(session);
    emailMessage.setFrom(new InternetAddress(mUserName));
    emailMessage.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(recipient));
    emailMessage.setSubject(subject);
    emailMessage.setText(body);

    try {
      Transport.send(emailMessage);
    } catch (MessagingException e) {
      e.printStackTrace();
      Log.d("HHHHH", "Failed to send email?");
    }
  }

  private Properties getProperties(){
    Properties props = new Properties();
    props.put("mail.smtp.host", mHostName);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.port", Integer.toString(mPort));
    return props;
  }

  private Properties getSSLProperties(){
    Properties props = new Properties();
    props.put("mail.smtp.host", mHostName);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.port", Integer.toString(mPort));
    props.put("mail.smtp.socketFactory.port", Integer.toString(mPort));
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    return props;
  }

  private Properties getTLSProperties() {
    Properties props = new Properties();
    props.put("mail.smtp.host", mHostName);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.port", Integer.toString(mPort));
    props.put("mail.smtp.socketFactory.port", Integer.toString(mPort));
    props.put("mail.smtp.starttls.enable", "true");
    return props;
  }

  @Override
  public PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(mUserName, mPassword);
  }
}
