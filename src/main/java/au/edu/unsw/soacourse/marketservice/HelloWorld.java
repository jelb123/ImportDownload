package au.edu.unsw.soacourse.marketservice;

import javax.jws.WebService;

@WebService
public interface HelloWorld {
    String sayHi(String text);
}

