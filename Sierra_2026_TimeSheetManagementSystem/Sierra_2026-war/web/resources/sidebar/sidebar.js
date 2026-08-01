window.SierraComponents = window.SierraComponents || {};

window.SierraComponents.sidebar = {
    toggle: function (widgetVar) {
        var sidebar = PF(widgetVar);
        if (sidebar) {
            sidebar.toggle();
        }
    }
};
