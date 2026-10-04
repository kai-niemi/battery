foreach ([1+1,2+2,3+3]) {
    y = _x;
    foreach (["a","b","c"]) {
        log.info ("%s, %s", [_x, y]);
    }
}
