$('document').ready(function () {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    })
    
    var editor1 = CodeMirror.fromTextArea(document.getElementById('input'), {
        lineNumbers: true,
        mode: 'text/x-perl',
        theme: 'abbott',
    });

    editor1.markText({line: formModel.errorFromLine - 1, ch: formModel.errorFromChar},
        {line: formModel.errorFromLine - 1, ch: formModel.errorFromChar+10},
        {css: "background:red;color:yellow;font-weight:bold;"});

    CodeMirror.fromTextArea(document.getElementById('output'), {
        lineNumbers: true,
        lineWrapping: true,
        readOnly: true,
        mode: 'text/x-perl',
        theme: 'dracula',
    });
});

