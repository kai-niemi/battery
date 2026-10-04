for k from 1 to 10 {
    log.info ("k=%s", [k]);

    for j from k to (k+5) {
        log.info ("  j=%s k=%s", [j, k]);
    }
}
