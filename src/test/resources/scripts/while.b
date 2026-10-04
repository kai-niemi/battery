x=0;
y=10;
while (x<10) {
    log.info ("x: " + x);
    while (y<15) {
      log.info ("y: " + y);
      y = y + 1;
    }
    y = 10;
    x = x + 1;
}

