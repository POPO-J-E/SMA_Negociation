package madkit.marketorg.negociation;

import madkit.kernel.Message;
import madkit.marketorg.Client;
import madkit.marketorg.model.Request;

import java.util.List;

/**
 * Created by kifkif on 06/12/2017.
 */
public class StaticRequestNegociation extends AbstractRequestNegociation
{
    @Override
    public Request updateNewRequest(Client client, List<Message> bids) {
        return client.getLastRequest();
    }
}
