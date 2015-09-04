
package au.edu.unsw.soacourse.marketservice;

import javax.jws.WebService;

@WebService(endpointInterface = "au.edu.unsw.soacourse.marketservice.HelloWorld")
public class HelloWorldImpl implements HelloWorld {

    public String sayHi(String text) {
        return "Hello " + text;
    }
}

