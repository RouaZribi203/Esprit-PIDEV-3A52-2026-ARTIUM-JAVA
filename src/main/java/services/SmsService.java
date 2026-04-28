package services;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
public class SmsService {
    private static final String ACCOUNT_SID = "ACb5cdf4b11f253b7bd0c43d4f4527b045";
    private static final String AUTH_TOKEN = "e45b6ffbb4e6d1edb46b4dc2157152d2";
    private static final String FROM_NUMBER = "+19785849858 ";

    public SmsService() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public void sendSms(String to, String messageText) {
        Message message = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(FROM_NUMBER),
                messageText
        ).create();

        System.out.println("SMS sent with SID: " + message.getSid());
    }
}
