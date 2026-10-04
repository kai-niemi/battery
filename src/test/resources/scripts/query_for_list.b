r = jdbc.queryForList("select * from account limit 10");
foreach (r) {
    log.info ("%s", [_x.keySet()] );
}