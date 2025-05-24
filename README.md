# ext-iv-quick-access

## What is this?

This is an extension for ImageViewer to provide quick access to frequently used directories. This allows you
to bypass the popup menu and just click a button in a conveniently located quick-access panel to sort the
current image to a given directory.

### How do I use it?

Clone the repo and build the extension jar with maven:

```shell
git clone https://github.com/scorbo2/ext-iv-quick-access.git
cd ext-iv-quick-access
maven package
```

Now you can copy the extension jar to wherever you keep your imageviewer extensions:

```shell
cp target/ext-iv-quick-access-2.1.0-SNAPSHOT.jar ~/.ImageViewer/extensions/
```

Now restart imageviewer and you should find a "Quick Access" button on the Quick Move
Destinations configuration dialog:

![Screenshot1](screenshot1.png "Screenshot 1")

You can add directories here, recursively if needed, and even rename them for labelling purposes.
The Quick Access panel will show up by default on the left side of the main image panel (you can configure
the location of the Quick Access panel in application settings):

![Screenshot2](screenshot2.png "Screenshot 2")

Now, instead of going through the menus, you can simply click one of the destination buttons, and the
current image will be sorted into that destination directory.

Click "Remove group" on any of the destination groupings to remove it from the Quick Access panel.
You can easily get it back by revisiting the Quick Access Destinations configuration dialog. 

Note that the hierarchy of destinations presented on the Quick Access panel doesn't have to match the
hierarchy of directories on the file system! Using the Quick Access Destinations configuration dialog,
you can create whatever hierarchy makes sense, regardless of how the file system is laid out.

### Requirements

ImageViewer 2.1 or higher.

### License

Imageviewer and this extension are made available under the MIT license: https://opensource.org/license/mit
