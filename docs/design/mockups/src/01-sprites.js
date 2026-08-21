// Sprites at the exact sizes of docs/design/02-sprite-sizes.md. Some are still block silhouettes
// standing in for art that has not been drawn; those are marked. What has to be right in every
// case is the footprint.
//
// Every sprite here is symmetric about its centre column, so wide ones are authored as half rows
// and mirrored by sym(). A hand-typed 23-wide row is a width error waiting to happen and the half
// is half the chances; more importantly the mirror cannot drift, which is what actually goes wrong
// when a big sprite is typed out in full.
function sym(rows) {
  return rows.map(half => half + half.slice(0, half.length - 1).split('').reverse().join(''));
}

const SPRITES = {
  'ship-basic': { w: 15, h: 17, r: 3.0, art: [
    '.......k.......',
    '......klk......',
    '......klk......',
    '.....klllk.....',
    '.....klslk.....',
    '....klssslk....',
    '....klssslk....',
    '...kklssslkk...',
    '...klssssslk...',
    '.klklssssslklk.',
    '.koklssssslkok.',
    '.klklssssslklk.',
    '..kklssssslkk..',
    '...klssssslk...',
    '....klcCclk....',
    '.....kcCck.....',
    '......kCk......'
  ] },
  'enemy-basic': { w: 13, h: 13, r: 5.5, art: [
    '....kkkkk....',
    '...kvvvvvk...',
    '..kvvvvvvvk..',
    '.kvvvlllvvvk.',
    'kvvvlOOOlvvvk',
    'kvvlOOOOOlvvk',
    'kvvvlOOOlvvvk',
    'kvvvvlllvvvvk',
    '.kvvvvvvvvvk.',
    '..kvvvkvvvk..',
    '...kvk.kvk...',
    '...kOk.kOk...',
    '....k...k....'
  ] },
  'enemy-light': { w: 11, h: 13, r: 4.5, art: [
    '.....k.....',
    '....kvk....',
    '....kvk....',
    '...kvvvk...',
    '...kvlvk...',
    '..kvvlvvk..',
    '..kvlOlvk..',
    '.kvvlOlvvk.',
    'kvvvlllvvvk',
    'kvk.kvk.kvk',
    'kOk.kvk.kOk',
    '..k.kvk.k..',
    '....kOk....'
  ] },
  'enemy-shooter': { w: 15, h: 15, r: 6.5, art: [
    '.....kkkkk.....',
    '....kvvvvvk....',
    '...kvvvvvvvk...',
    '..kvvvlllvvvk..',
    '.kvvvlOOOlvvvk.',
    'kvvvlOOfOOlvvvk',
    'kvvvlOOOOOlvvvk',
    'kvvvvlOOOlvvvvk',
    'kvvvvvlllvvvvvk',
    '.kvvvvvvvvvvvk.',
    '..kvvvvvvvvvk..',
    '.kvk.kvvvk.kvk.',
    '.kOk..kvk..kOk.',
    '..k...kOk...k..',
    '......k.k......'
  ] },
  'enemy-rush': { w: 9, h: 15, r: 4.0, art: [
    '....k....',
    '...kvk...',
    '...kvk...',
    '...kvk...',
    '..kvvvk..',
    '..kvlvk..',
    '..kvlvk..',
    '.kvvlvvk.',
    '.kvvlvvk.',
    'kvvvlvvvk',
    'kvk.k.kvk',
    'kOk.k.kOk',
    '.k..k..k.',
    '...kOk...',
    '....O....'
  ] },
  // The tank is where the one-violet constraint was tested, because it is the first sprite wide
  // enough for a flat hull to show. It holds: V4 is the mass, N5 the shoulder armour and the rib
  // that separates the two, N6 only the top-lit crest. The interior shadow is N0 and there is no
  // darker violet to miss, because at 23 px the black reads as a panel joint rather than a hole in
  // the shading. Its contour is the point — hard shoulders, a tapering chin and long legs, so it
  // is not a large enemy-basic when four other things are on screen.
  'enemy-tank': { w: 23, h: 23, r: 10.5, art: sym([
    '.........kkk',
    '........klll',
    '.......kllll',
    '......klssss',
    '.....klsvvvv',
    '....klsvvvvv',
    'ksssksvvvvvv',
    'ksOsksvvvvoo',
    'ksssksvvvoOf',
    'ksssksvvvvoo',
    'ksssksvvvvvv',
    'kkkkksvvvvvv',
    '.ksvvvvvvvvv',
    '..ksvvvvvvvv',
    '...ksvvvvvvv',
    '....ksvvvvvv',
    '.....ksvvvvv',
    '......ksvvvv',
    '......kvkkkk',
    '......kvk...',
    '......kOk...',
    '......kOk...',
    '.......O....'
  ]) },
  'shot-p1': { w: 3, h: 9, r: 1.5, art: [
    'kck', 'kCk', 'kCk', 'kCk', 'kCk', 'kCk', 'kCk', 'kck', '.k.'
  ] },
  'shot-p2': { w: 5, h: 11, r: 2.0, art: [
    '..k..', '.kCk.', '.kCk.', 'kCwCk', 'kCwCk', 'kCwCk', 'kCwCk', 'kCwCk', '.kCk.', '.kCk.', '..k..'
  ] },
  'shot-e-small': { w: 5, h: 5, r: 2.0, art: [
    '.hhh.', 'hHHHh', 'hHXHh', 'hHHHh', '.hhh.'
  ] },
  'shot-e-heavy': { w: 7, h: 7, r: 3.0, art: [
    '..hhh..', '.hHHHh.', 'hHHXHHh', 'hHXXXHh', 'hHHXHHh', '.hHHHh.', '..hhh..'
  ] },
  'shot-e-bolt': { w: 5, h: 11, r: 2.0, art: [
    '..h..', '.hHh.', '.hXh.', '.hXh.', '.hXh.', '.hXh.', '.hXh.', '.hXh.', '.hHh.', '.hHh.', '..h..'
  ] },
  'pickup-weapon': { w: 11, h: 11, r: 6.0, art: [
    '...kkkkk...',
    '..kGGGGGk..',
    '.kGgggggGk.',
    'kGggwgwggGk',
    'kGgwgggwgGk',
    'kGgggggggGk',
    'kGggwgwggGk',
    'kGgwgggwgGk',
    '.kGgggggGk.',
    '..kGGGGGk..',
    '...kkkkk...'
  ] },
  'icon-life': { w: 9, h: 9, art: [
    '....k....',
    '...klk...',
    '..klllk..',
    '..klslk..',
    '.klssslk.',
    '.klssslk.',
    'kklssslkk',
    '.k.kck.k.',
    '....C....'
  ] },
  'icon-bomb': { w: 9, h: 9, art: [
    '...kkk...',
    '.kklllkk.',
    '.klOOOlk.',
    'klOOfOOlk',
    'klOfffOlk',
    'klOOfOOlk',
    '.klOOOlk.',
    '.kklllkk.',
    '...kkk...'
  ] },
  'icon-shield': { w: 13, h: 13, art: [
    '..kkkkkkkkk..',
    '.klllllllllk.',
    'klccccccccclk',
    'klcclllllcclk',
    'klcclwwwlcclk',
    'klcclllllcclk',
    'klccccccccclk',
    '.klccccccclk.',
    '..klccccclk..',
    '...klccclk...',
    '....klclk....',
    '.....klk.....',
    '......k......'
  ] },
  'icon-invuln': { w: 13, h: 13, art: [
    '......k......',
    '......O......',
    '..k...O...k..',
    '...O..O..O...',
    '....O.O.O....',
    '.....OOO.....',
    'kOOOOOfOOOOOk',
    '.....OOO.....',
    '....O.O.O....',
    '...O..O..O...',
    '..k...O...k..',
    '......O......',
    '......k......'
  ] },
  'icon-module': { w: 17, h: 17, art: [
    '....k.......k....',
    '...klk.....klk...',
    '...klk.....klk...',
    '..klslk...klslk..',
    '..klslk...klslk..',
    '..klslk...klslk..',
    '..klslk...klslk..',
    '..klslk...klslk..',
    '..klolk...klolk..',
    '..klslk...klslk..',
    '..klslk...klslk..',
    '..kkslkk.kkslkk..',
    '..k.k.k...k.k.k..',
    '...kOk.....kOk...',
    '...kfk.....kfk...',
    '....O.......O....',
    '.................'
  ] }
};

// Every sprite must match the size it declares, and every character must be a palette colour.
// A silhouette that lies about its footprint is the exact failure this mock exists to catch.
function validateSprites() {
  const problems = [];
  for (const [id, s] of Object.entries(SPRITES)) {
    if (s.art.length !== s.h) {
      problems.push(id + ': declares height ' + s.h + ' and has ' + s.art.length + ' rows');
    }
    s.art.forEach((row, y) => {
      if (row.length !== s.w) {
        problems.push(id + ': row ' + y + ' declares width ' + s.w + ' and is ' + row.length);
      }
      for (const ch of row) {
        if (CHARS[ch] === undefined) problems.push(id + ': unknown character "' + ch + '"');
      }
    });
    if (s.w % 2 === 0 || s.h % 2 === 0) {
      problems.push(id + ': ' + s.w + 'x' + s.h + ' has an even dimension, breaking the odd rule');
    }
  }
  return problems;
}
