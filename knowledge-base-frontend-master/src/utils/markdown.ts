/**
 * Markdown utility functions
 */

/**
 * Normalizes Markdown headings: inserts a space after `#` when one is missing.
 *
 * <p>The CommonMark/GFM spec requires a space after `#` for it to be recognized as a heading.
 * LLM output often produces headings like `###Heading` with no space, which then renders as a
 * plain paragraph instead.</p>
 *
 * @param text Raw Markdown text
 * @returns Markdown text with heading syntax normalized
 */
export const normalizeHeadings = (text: string): string => {
  if (!text) return text;
  // 1-6 leading '#' characters immediately followed by a non-space, non-'#' character → insert a space between them
  return text.replace(/^(#{1,6})([^\s#])/gm, '$1 $2');
};

/**
 * Normalizes Markdown content to prevent pasted indentation from causing headings to be
 * mis-parsed as code blocks.
 *
 * <p>When a user copies content from a webpage, IDE, or document into the editor, it may come
 * with leading indentation. The CommonMark spec treats lines starting with 4 or more spaces as
 * indented code blocks, which causes Markdown syntax such as headings (#) to render as code
 * instead of headings.</p>
 *
 * <p>Approach: find the smallest common indentation across all non-empty lines and strip it from
 * the start of every line (similar to Python's textwrap.dedent). If any non-empty line has no
 * leading whitespace, no changes are made.</p>
 *
 * @param text Raw Markdown text
 * @returns Markdown text with the common indentation removed
 */
export const normalizeMarkdown = (text: string): string => {
  if (!text) return text;
  const lines = text.split('\n');

  // Find the smallest indentation among all non-empty lines
  let minIndent = Infinity;
  for (const line of lines) {
    if (line.trim().length === 0) continue;
    const match = line.match(/^[ \t]*/);
    if (match) minIndent = Math.min(minIndent, match[0].length);
  }

  // No indentation or no content — nothing to do
  if (minIndent === Infinity || minIndent === 0) return text;

  // Remove the common indentation
  return lines
    .map(line => {
      if (line.trim().length === 0) return line;
      return line.slice(Math.min(minIndent, line.length));
    })
    .join('\n');
};

/**
 * Automatically wraps directory tree structures in a Markdown code block.
 *
 * <p>LLM-generated directory trees such as {@code com.example.demo ├──controller/ │ └── User.java}
 * use box-drawing characters (├└│─) but aren't wrapped in ``` fences, so Markdown renders them as
 * a plain paragraph — collapsing spaces and dropping line breaks, making the output unreadable.</p>
 *
 * <p>This function detects 2 or more consecutive lines containing box-drawing characters and
 * automatically wraps them in a ``` code block.</p>
 *
 * @param text Raw Markdown text
 * @returns Markdown text with directory trees automatically wrapped
 */
export const normalizeDirTree = (text: string): string => {
  if (!text) return text;
  if (/```|<pre>/.test(text)) return text; // Already has a code block, skip

  const TREE = /[├└│─]/;
  const lines = text.split('\n');
  const out: string[] = [];
  let i = 0;

  while (i < lines.length) {
    // Lines without box-drawing characters → output as-is
    if (!TREE.test(lines[i])) {
      out.push(lines[i]);
      i++;
      continue;
    }

    // Collect consecutive lines containing box-drawing characters
    const start = i;
    while (i < lines.length && TREE.test(lines[i])) i++;

    // Insert a line break right before whitespace followed by ├ or └, splitting up entries crammed onto one line
    const expanded = lines
      .slice(start, i)
      .map((l) => l.replace(/[ \t]+([├└])/g, '\n$1'))
      .join('\n');

    out.push(
      '\n\n<pre style="background:#1e1e1e;color:#d4d4d4;padding:10px 14px;border-radius:6px;overflow-x:auto;font-size:13px;line-height:1.55;font-family:monospace;">\n' +
      expanded +
      '\n</pre>\n\n',
    );
  }

  return out.join('\n');
};

/**
 * One-stop Markdown normalization for AI streaming output.
 *
 * <p>Chains normalizeHeadings + normalizeDirTree to ensure headings, directory structures, and
 * similar content in AI output render correctly.</p>
 */
export const prepareStreamingMarkdown = (text: string): string => {
  if (!text) return text;
  return normalizeDirTree(normalizeHeadings(text));
};
