console.log('Preparing to be cool...');

function render(queryConfig, div) {
    Ext4.onReady(function() {
        console.log('ready...');
        div.innerHTML = 'Less cool than expected.';
    });
    console.log('render called.');
}