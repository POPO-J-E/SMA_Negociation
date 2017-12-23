package madkit.marketorg.negociation;

import madkit.marketorg.model.Offer;
import madkit.marketorg.model.Request;

/**
 * Created by kifkif on 06/12/2017.
 */
public class StaticNegociation extends AbstractNegoctiation {

    @Override
    public Offer updateNewOffer(Request request, Offer lastOffer, String competence, String provider) {
        return lastOffer;
    }
}
