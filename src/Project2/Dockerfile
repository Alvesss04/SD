FROM nunopreguica/sd2526tpbase

WORKDIR /home/sd

ADD hibernate.cfg.xml .
ADD messages.props .

COPY tls/*.ks /home/sd/
COPY tls/*.ts /home/sd/

COPY target/sd*.jar sd2526.jar
